/**
 * Abstract interface for a KeyValue store
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

/**
 * This is the simple interface that all of the KeyValue servers,
 * Caches and Stores should implement.
 *
 */
public interface KeyValueInterface {
	/**
	 * Insert Key, Value pair into the storage unit 
	 * @param key is the object used to index into the store
	 * @param value is the object corresponding to a unique key
	 * @return whether a value was overwritten when inserting the new data tuple
	 * @throws IOException is thrown when there is an error when inserting the entry into the store 
	 */
	public boolean put(String key, String value) throws KVException;
	
	/**
	 * Retrieve the object corresponding to the provided key 
	 * @param key is the object used to index into the store
	 * @return the value corresponding to the provided key
	 * @throws KVException if there is an error when looking up the object store
	 */
	public String get(String key) throws KVException;
	
	/**
	 * Delete the object corresponding to the provided key 
	 * @param key is the object used to index into the store
	 * @throws KVException if there is an error when looking up the object store
	 */	
	public void del(String key) throws KVException;	
}
