/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.workflow;

import junit.framework.TestCase;

public class FactTest extends TestCase
{

    public void testCreate()
    {
        Fact f1 = new Fact("fact1",new Integer(1));
        Fact f2 = new Fact("fact2","A factual String");
        Fact f3 = new Fact("fact3",Outcome.DECISION_ACKNOWLEDGE);

        assertEquals("fact1", f1.getMessageKey());
        assertEquals("fact2", f2.getMessageKey());
        assertEquals("fact3", f3.getMessageKey());

        assertEquals(new Integer(1), f1.getValue());
        assertEquals("A factual String", f2.getValue());
        assertEquals(Outcome.DECISION_ACKNOWLEDGE, f3.getValue());
    }

}
