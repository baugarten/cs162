package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Lib;
import java.util.*;

public class Boat
{
    static BoatGrader bg;
 
    static int SURRENDER_WAIT_TIME = 	50000;
    static int CHILD_ORIGIN_WAIT_TIME = 1000;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	//System.out.println("\n ***Testing Boats with only 2 children***");
	//begin(0, 2, b);

	//System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
  	//begin(1, 2, b);

  	//System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	//begin(3, 3, b);
	
	//System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(246, 2, b);
    }
    
    Lock globalMutex = new Lock();
    Communicator endReporter = new Communicator();
    int childrenAtDest = 0;	// the number of children at the dest as tracked
    						// by the people at the origin
    public class Island {
    	Condition waitingPeople;
    	int surrenderCounter = 0;
    }
    Island originIsland;
    Island destIsland;
    
    
    //ArrayList<Person> plist = new ArrayList<Person>();
    public class ABoat {
    	Island loc;
    	boolean canBoard = true;
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
    			return !(pilot instanceof Adult)
        				&& (pilot == null || passenger == null);
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
    	// returns true if embarked as pilot
    	boolean embark(Person person) {
    		Lib.assertTrue(person.loc == loc);
    		if (person instanceof Adult) {
    			Lib.assertTrue(pilot==null);
    			Lib.assertTrue(passenger==null);
    			pilot = person;
    			return true;
    		} else if (person instanceof Child) {
    			if (passenger==null) {
    				passenger = person;
    				return false;
    			} else if (pilot==null) {
    				pilot = person;
    				return true;
    			} else {
    				Lib.assertTrue(false);
    				return false;
    			}
    		} else {
    			Lib.assertTrue(false);
    			return false;
    		}
    	}
    	
    	// embarks a child as a pilot
    	void embarkAsPilot(Person person) {
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
    			if (theBoat.isOnBoat(this)) {
        			if (loc == originIsland) {
        				// arrived at origin
        				dbg("arrive origin");    				
        				boatArriveAtOrigin();
        			} else if (loc == destIsland) {
        				// arrived at destination
        				dbg("arrive dest");
        				boatArriveAtDest();
        			} else {
        				Lib.assertTrue(false);
        			}
    			} else {
    				if (loc.surrenderCounter > rememberedSurrenderCounter) {
    					rememberedSurrenderCounter = loc.surrenderCounter;
    					
        				int island = (loc == destIsland)? 1 : 0;
        				int comms = rememberedSurrenderCounter * 2 + island;
        				endReporter.speak(comms);
        				dbg("Surrender " + island + " on counter " + rememberedSurrenderCounter);
        				
        				if (loc == destIsland) {
	        				globalMutex.release();
	        				ThreadedKernel.alarm.waitUntil(SURRENDER_WAIT_TIME);
	        				globalMutex.acquire();
        				}
    				}
    				if (loc == originIsland) {
        				// on origin
        				dbg("origin logic");
        				originLogic();
        			} else if (loc == destIsland) {
        				// on destination
        				dbg("dest logic");
        				destLogic();
        			} else {
        				Lib.assertTrue(false);
        			}
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
    	
    	void dbg(String msg) {
    		System.out.println(KThread.currentThread().getName() + ": " + msg);
    	}
    }

    public class Adult extends Person {
    	void boatArriveAtOrigin() {
    		// this should never happen!
    		Lib.assertTrue(false);
    	}
    	void boatArriveAtDest() {
    		bg.AdultRowToMolokai();
    		dbg("arrived at dest");
    		
    		theBoat.disembark(this);
    		loc.waitingPeople.wakeAll();
    		loc.waitingPeople.sleep();
    	}
    	
    	void originLogic() {
    		if (childrenAtDest > 0 && theBoat.canEmbark(this)) {
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
    		dbg("arrived at origin");
    		
    		theBoat.disembark(this);
    		childrenAtDest--;
    		dbg("child origin arrive " + childrenAtDest);
    		loc.waitingPeople.wakeAll();
    		int lastBoatRunCounter = theBoat.runCounter;
    		
    		globalMutex.release();
	    	ThreadedKernel.alarm.waitUntil(CHILD_ORIGIN_WAIT_TIME);
	    	globalMutex.acquire();
	    	
	    	if (theBoat.runCounter == lastBoatRunCounter) {
	    		if (theBoat.canEmbark(this)) {
	    			if (theBoat.isEmpty()) {
	    				theBoat.embarkAsPilot(this);
	    				childrenAtDest++;
	    				
	    				while (childrenAtDest <= 1) {
	    					loc.waitingPeople.wakeAll();
	    					globalMutex.release();
	    			    	ThreadedKernel.alarm.waitUntil(CHILD_ORIGIN_WAIT_TIME);
	    			    	globalMutex.acquire();
	    				}
	    				
	    				loc.surrenderCounter++;
	    				dbg("Raising surrender counter to " + loc.surrenderCounter);
	    				theBoat.surrenderCounter = loc.surrenderCounter;
	    				
	    				row_row_row_the_boat(destIsland);
		        		// fall through to next loop, run arrival logic
	    			} else {
		        		// fall through to next loop, run origin logic and board the boat
	    			}
	    		} else {
	    			// should never have a situation where I can't board the boat
	    			// and it hasn't left
	    			Lib.assertTrue(false);
	    		}
	    	} else {
	    		loc.waitingPeople.sleep();
	    	}
    	} 
    	
    	void boatArriveAtDest() {
    		if (theBoat.isPassenger(this)) {
    			bg.ChildRideToMolokai();
    		} else if (theBoat.isPilot(this)) {
    			bg.ChildRowToMolokai();    			
    		}
    		dbg("arrived at dest");
    		
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
				dbg("passenger wake");
				theBoat.waitingPassenger.wakeAll();
				dbg("destination sleep");
				loc.waitingPeople.sleep();
				dbg("destination woken");
			}
    	}
    	
    	void originLogic() {
    		if (theBoat.canEmbark(this)) {
    			dbg("origin board");
    			boolean isPilot = theBoat.embark(this);
    			childrenAtDest++;

    			if (isPilot) {
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
    		if (theBoat.isEmpty() && theBoat.canEmbark(this)) {
    			theBoat.embarkAsPilot(this);
    			row_row_row_the_boat(originIsland);
    			// fall through to next loop
    		} else {
    			loc.waitingPeople.sleep();
    		}
    	}
    }
    
    public void instanceBegin( int adults, int children, BoatGrader b ) {	
    	globalMutex = new Lock();
    	
    	originIsland = new Island();
    	originIsland.waitingPeople = new Condition(globalMutex);
    	destIsland = new Island();
    	destIsland.waitingPeople = new Condition(globalMutex);
    	
    	theBoat = new ABoat();
    	theBoat.loc = originIsland;
    	theBoat.waitingPassenger = new Condition(globalMutex);
    	
    	for (int i=0;i<adults;i++) {
    		Person thisGuy = new Adult();
    		//plist.add(thisGuy);
    		thisGuy.loc = originIsland;
    		KThread t = new KThread(thisGuy);
    		t.setName("Adult " + i);
            t.fork();
            System.out.println("Forked " + t.getName());
    	}
    	for (int i=0;i<children;i++) {
    		Person thisGuy = new Child();
    		//plist.add(thisGuy);
    		thisGuy.loc = originIsland;
    		KThread t = new KThread(thisGuy);
    		t.setName("Child " + i);
            t.fork();
            System.out.println("Forked " + t.getName());
    	}
    	
    	ArrayList<Integer> destReplies = new ArrayList<Integer>();
    	//ArrayList<Boolean> failed = new ArrayList<Boolean>();
    	int lastRun = 0;
    	int counter = 0;

    	while (true) {
    		//KThread.yield();
    		int recv = endReporter.listen();
    		int island = recv % 2;
    		int run = recv / 2;
    		
    		if (run > lastRun) {
    			lastRun = run;
    			counter = 0;
    		} else if (run < lastRun) {
    			continue;
    		}
    		if (island == 1) {
    			counter++;
    			if (counter == adults+children) {
    				break;
    			}
    		}
    	}
    	/*System.out.println("Final test");
      	for (Person p : plist) {
      		if (p.loc != destIsland) {
      			System.out.println("FAILED************");
      		} else {
      		//	System.out.println("       OK");
      		}
      				
        }*/

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
