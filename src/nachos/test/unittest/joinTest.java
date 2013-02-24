package nachos.test.unittest;

import static org.junit.Assert.*;

import nachos.threads.KThread;

import org.junit.Test;

public class joinTest extends TestHarness{

	@Test
	public void testRunNachos(){
		assertTrue(true);
	}
	
	@Test
	public void testJoin1() throws Exception {
		System.out.println("--------------------------------\nTEST JOIN");
		
		/* Case 1: x joins y; x runs first */
		enqueueJob(new Runnable(){
			@Override
			public void run(){
				
				final KThread joinee = new KThread(new Joinee()).setName("Joinee");
				final KThread joiner = new KThread(new Joiner(joinee)).setName("Joiner");
				System.out.println("\n--Case1: x joins y and x runs first--");
				joiner.fork();
				joinee.fork();
			}
		});
		
		/* Case 2: x joins y; y runs first */
		enqueueJob(new Runnable(){
			@Override
			public void run(){

				final KThread joinee = new KThread(new Joinee()).setName("Joinee");
				final KThread joiner = new KThread(new Joiner(joinee)).setName("Joiner");
				System.out.println("\n--Case2: x joins y and y runs first--");
				joinee.fork();
				joiner.fork();
			}
		});
		
		/*Case3: x and y join on z; z must finish first then either x or y finishes */
		enqueueJob(new Runnable(){
			@Override
			public void run(){

				final KThread joineeZ = new KThread(new Joinee()).setName("JoineeZ");
				final KThread joinerY = new KThread(new Joiner(joineeZ)).setName("JoinerY");
				final KThread joinerX = new KThread(new Joiner(joineeZ)).setName("JoinerX");
				System.out.println("\n--Case3: x and y join on z; z must finishs first then either x or y finishes--");
				joineeZ.fork();
				joinerX.fork();
				joinerY.fork();
			}
		});
		
		/*Case4: super joiner x joins y and z; y and z must finish before x */ 
		enqueueJob(new Runnable(){
			@Override
			public void run(){
				
				final KThread joineeZ = new KThread(new Joinee()).setName("JoineeZ");
				final KThread joineeY = new KThread(new Joinee()).setName("JoineeY");
				final KThread joinerX = new KThread(new SuperJoiner(joineeY, joineeZ)).setName("JoinerX");
				System.out.println("\n--Case4: super joiner x joins y and z; y and z must finish before x--");
				joineeZ.fork();
				joinerX.fork();
				joineeY.fork();
			}
		});
		
		
	}
	
	private static class Joiner implements Runnable {
		private KThread joinee;
		
		Joiner(KThread joiNee){
			joinee = joiNee;
		}
		
		public void run(){
			System.out.println("Joiner: before joining" + joinee.getName());
			joinee.join();
			System.out.println("Joiner: after joining" + joinee.getName());
		}
	}
	
	private static class Joinee implements Runnable {
		public void run(){
			System.out.println("Joinee: Happy running");
		}
	}
	
	private static class SuperJoiner implements Runnable {
		private KThread joinee1, joinee2;
		
		SuperJoiner(KThread joiNee1, KThread joiNee2){
			joinee1 = joiNee1;
			joinee2 = joiNee2;
		}
		
		public void run(){
			System.out.println("Joiner: before joining" + joinee1.getName());
			joinee1.join();
			System.out.println("Joiner: after joining" + joinee1.getName());
			System.out.println("Joiner: before joining" + joinee2.getName());
			joinee2.join();
			System.out.println("Joiner: after joining" + joinee2.getName());
		}
	}

}
