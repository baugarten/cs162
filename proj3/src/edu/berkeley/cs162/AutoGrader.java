/**
 * Autograder for the Key-Value Store.
 * 
 * @author Mosharaf Chowdhury (http://www.mosharaf.com)
 * @author Prashanth Mohan (http://www.cs.berkeley.edu/~prmohan)
 * 
 * Copyright (c) 2012, University of California at Berkeley
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of University of California, Berkeley nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *    
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs162;

import java.io.IOException;
import java.util.ArrayList;

public class AutoGrader {
	
	private static long STORE_DELAY = 1000;
	private static long CACHE_DELAY = 10;
	
	private static KVStore dataStore = null;
	private static KVCache dataCache = null;
	
	private static int currentOp = 0;
	
	public static void registerKVServer(KVStore dataStore, KVCache dataCache) {
		AutoGrader.dataStore = dataStore;
		AutoGrader.dataCache = dataCache;
	}

	public static void agCachePutStarted(String key, String value) {
		
	}
	
	public static void agCachePutFinished(String key, String value) {
		
	}

	public static void agCacheGetStarted(String key) {
		
	}
	
	public static void agCacheGetFinished(String key) {
		
	}

	public static void agCacheDelStarted(String key) {
		
	}
	
	public static void agCacheDelFinished(String key) {
		
	}

	public static void agStorePutStarted(String key, String value) {
		
	}
	
	public static void agStorePutFinished(String key, String value) {
		
	}

	public static void agStoreGetStarted(String key) {
		
	}
	
	public static void agStoreGetFinished(String key) {
		
	}

	public static void agStoreDelStarted(String key) {
		
	}

	public static void agStoreDelFinished(String key) {
		
	}

	public static void agKVServerPutStarted(String key, String value) {
		
	}
	
	public static void agKVServerPutFinished(String key, String value) {
		
	}

	public static void agKVServerGetStarted(String key) {
		
	}
	
	public static void agKVServerGetFinished(String key) {
		
	}
	
	public static void agKVServerDelStarted(String key) {
		
	}

	public static void agKVServerDelFinished(String key) {
		
	}

	public static void agCachePutDelay() {
		delay(CACHE_DELAY);
	}

	public static void agCacheGetDelay() {
		delay(CACHE_DELAY);
	}
	
	public static void agCacheDelDelay() {
		delay(CACHE_DELAY);
	}

	/**
	 * KVStore will sleep for STORE_DELAY milliseconds  
	 */
	public static void agStoreDelay() {
		delay(STORE_DELAY);
	}
	
	/**
	 * Helper method to put the current thread to sleep for sleepTime duration
	 * @param sleepTime time to sleep in milliseconds
	 */
	private static void delay(long sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
