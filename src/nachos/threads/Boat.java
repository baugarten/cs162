package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Lib;
import java.util.*;

public class Boat
{
    static BoatGrader bg;
 
    static int SURRENDER_WAIT_TIME = 50;
    static int CHILD_ORIGIN_WAIT_TIME = 5;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    static Alarm globalAlarm;
    Lock globalMutex = new Lock();
    Communicator endReporter = new Communicator();
    
    public class Island {
    	Condition waitingPeople;
    	int surrenderCounter = 0;
    }
    Island originIsland;
    Island destIsland;
    
    public class ABoat {
    	Island loc;
    	boolean canBoard = true;
    	boolean flag = false;	// false = odd, true = even
    	int runCounter = 0;
    	int surrenderCounter = 0;
    	
    	Condition waitingPassenger;
    	Person pilot = null;
    	Person passenger = null;
    	
    	boolean canEmbark(Person person) {
    		if (loc != person.loc) {
    			return false;
    		}
    		if (!canBoard) {
    			return false;
    		}
    		
    		if (person instanceof Adult) {
    			return (pilot==null && passenger==null);
    		} else if (person instanceof Child) {
    			return pilot==null
        				|| pilot instanceof Adult;
    		} else {
    			Lib.assertTrue(false);
    			return false;
    		}
    	}
    	boolean isFull() {
    		return pilot!=null && passenger!=null
    				|| pilot instanceof Adult;
    	}
    	boolean isEmpty() {
    		return pilot==null && passenger==null;
    	}
    	boolean isOnBoat(Person person) {
    		return pilot==person || passenger==person;
    	}
    	boolean isPilot(Person person) {
    		return pilot==person;
    	}
    	boolean isPassenger(Person person) {
    		return passenger==person;
    	}
    	
    	// person boards a boat
    	// once ready to depart, isFull returns true
    	void embark(Person person) {
    		Lib.assertTrue(person.loc == loc);
    		if (person instanceof Adult) {
    			Lib.assertTrue(pilot==null);
    			Lib.assertTrue(passenger==null);
    			pilot = person;
    		} else if (person instanceof Child) {
    			if (passenger==null) {
    				passenger = person;
    			} else if (pilot==null) {
    				pilot = person;
    			} else {
    				Lib.assertTrue(false);
    			}
    		} else {
    			Lib.assertTrue(false);
    		}
    	}
    	
    	// embarks a child as a pilot
    	void embarkPilot(Person person) {
    		Lib.assertTrue(person.loc == loc);
    		Lib.assertTrue(pilot==null);
			pilot = person;
    	}
    	
    	void disembark(Person person) {
    		Lib.assertTrue(person.loc == loc);
    		if (person == pilot) {
    			pilot = null;
    		} else if (person == passenger) {
    			passenger = null;
    		} else {
    			Lib.assertTrue(false);
    		}
    	}
    }
    ABoat theBoat;
    
    public abstract class Person implements Runnable {
    	Island loc;
    	int rememberedSurrenderCounter = 0;
    	
    	public void run() {
    		globalMutex.acquire();
    		while (true) {
    			// probable end condition check
    			while (loc.surrenderCounter > rememberedSurrenderCounter) {
    				int island = (loc == destIsland)? 1 : 0;
    				int comms = rememberedSurrenderCounter * 2 + island;
    				endReporter.speak(comms);
    				rememberedSurrenderCounter++;
    				globalAlarm.waitUntil(SURRENDER_WAIT_TIME);
    			}
    			if (theBoat.isOnBoat(this) && loc == originIsland) {
    				// arrived at origin
    				boatArriveAtOrigin();
    			} else if (theBoat.isOnBoat(this) && loc == destIsland) {
    				// arrived at destination
    				boatArriveAtDest();
    			} else if (!theBoat.isOnBoat(this) && loc == originIsland) {
    				// on origin
    				originLogic();
    			} else if (!theBoat.isOnBoat(this) && loc == originIsland) {
    				// on destination
    				destLogic();
    			} else {
    				Lib.assertTrue(false);
    			}
    		}
    	}
    	
    	abstract void boatArriveAtOrigin();
    	abstract void boatArriveAtDest();
    	abstract void originLogic();
    	abstract void destLogic();
    	
    	void row_row_row_the_boat(Island dest) {
    		Lib.assertTrue(dest != loc);
    		Lib.assertTrue(dest != theBoat.loc);
    		
    		theBoat.runCounter++;	// paint a number on the boat
    		
    		theBoat.loc = dest;
    		theBoat.pilot.loc = dest;
    		if (theBoat.passenger != null) {
    			theBoat.passenger.loc = dest;
    		}
    	}
    }

    public class Adult extends Person {
    	void boatArriveAtOrigin() {
    		// this should never happen!
    		Lib.assertTrue(false);
    	}
    	void boatArriveAtDest() {
    		bg.AdultRowToMolokai();
    		theBoat.disembark(this);
    		loc.waitingPeople.wakeAll();
    		loc.waitingPeople.sleep();
    	}
    	
    	void originLogic() {
    		if (theBoat.flag == true && theBoat.canEmbark(this)) {
    			theBoat.embark(this);
    			row_row_row_the_boat(destIsland);
    			// fall through to next loop
    		} else {
    			loc.waitingPeople.sleep();
    		}
    	}
    	void destLogic() {
    		loc.waitingPeople.sleep();
    	}
    }
    
    public class Child extends Person {
    	void boatArriveAtOrigin() {
    		Lib.assertTrue(theBoat.isPilot(this));
    		bg.ChildRowToOahu();
    		theBoat.disembark(this);
    		loc.waitingPeople.wakeAll();
    		int lastBoatRunCounter = theBoat.runCounter;
    		globalAlarm.waitUntil(CHILD_ORIGIN_WAIT_TIME);
    		
    		if (theBoat.runCounter > lastBoatRunCounter) {
    			if (theBoat.canEmbark(this)) {
    				if (theBoat.isEmpty()) {
    					loc.surrenderCounter++;
    					theBoat.surrenderCounter = loc.surrenderCounter;
    				}
    				theBoat.embark(this);
    				theBoat.canBoard = false;
    				row_row_row_the_boat(destIsland);
        			// fall through to next loop
    			} else {
    				Lib.assertTrue(false);    				
    			}
    		}
    	}
    	
    	void boatArriveAtDest() {
    		if (theBoat.isPassenger(this)) {
    			bg.ChildRideToMolokai();
    		} else if (theBoat.isPilot(this)) {
    			bg.ChildRowToMolokai();    			
    		}
			theBoat.disembark(this);
			loc.surrenderCounter = theBoat.surrenderCounter;
			if (theBoat.isEmpty()) {
				// no one left, release boat and wake up island
				theBoat.canBoard = true;
				loc.waitingPeople.wakeAll();
				// continue through and run destination logic
				// (to check surrender counters)
			} else {
				// wake up passenger
				theBoat.waitingPassenger.wakeAll();
				loc.waitingPeople.sleep();
			}
    	}
    	
    	void originLogic() {
    		if (theBoat.canEmbark(this)) {
    			theBoat.embark(this);
    			if (theBoat.isFull()) {
    				theBoat.canBoard = false;
    				row_row_row_the_boat(destIsland);
        			// fall through to next loop
    			} else {
    				theBoat.waitingPassenger.sleep();
    			}
    		} else {
    			loc.waitingPeople.sleep();
    		}
    	}
    	void destLogic() {
    		if (theBoat.isEmpty()) {
    			theBoat.embark(this);
    			theBoat.flag = !theBoat.flag;
    			row_row_row_the_boat(originIsland);
    			// fall through to next loop
    		} else {
    			loc.waitingPeople.sleep();
    		}
    	}
    }
    
    public void instanceBegin( int adults, int children, BoatGrader b ) {	
    	originIsland = new Island();
    	originIsland.waitingPeople = new Condition(globalMutex);
    	destIsland = new Island();
    	destIsland.waitingPeople = new Condition(globalMutex);
    	
    	theBoat = new ABoat();
    	theBoat.loc = originIsland;
    	theBoat.waitingPassenger = new Condition(globalMutex);
    	
    	for (int i=0;i<adults;i++) {
    		Person thisGuy = new Adult();
    		thisGuy.loc = originIsland;
    		KThread t = new KThread(thisGuy);
    		t.setName("Adult " + i);
            t.fork();
    	}
    	for (int i=0;i<children;i++) {
    		Person thisGuy = new Child();
    		thisGuy.loc = originIsland;
    		KThread t = new KThread(thisGuy);
    		t.setName("Child " + i);
            t.fork();
    	}
    	
    	ArrayList<Integer> destReplies = new ArrayList<Integer>();
    	//ArrayList<Boolean> failed = new ArrayList<Boolean>();
    	
    	while (true) {
    		int recv = endReporter.listen();
    		int island = recv % 2;
    		int run = recv / 2;
    		
    		while (destReplies.size() < run) {
    			destReplies.add(0);
    		}
    		if (island == 1) {
    			destReplies.set(run, destReplies.get(run)+1);
    			if (destReplies.get(run) == adults+children) {
    				break;
    			}
    		}
    	}
    }
    
    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
	Boat me = new Boat();
	
	me.instanceBegin(adults, children, b);
    }

    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    }

    static void ChildItinerary()
    {
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
