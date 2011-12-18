/*******************************************************************************
 * Copyright (c) 2011, Daniel Murphy
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
/**
 * Created at 5:34:33 PM Jul 17, 2010
 */
package ubc.swim.gui;

import java.util.Arrays;

import ubc.swim.tests.BasicSwimTest;
import ubc.swim.tests.PaddleTest;


/**
 * Hard-coded list of tests that appear in swimmer app dropdown.
 * Adapted from org.jbox2d.testbed.framework.TestList by Daniel Murphy
 * 
 * @author Ben Humberston
 */
public class SwimTestList {
  
  public static void populateModel(SwimModel argModel){
      
	  argModel.addCategory("Basic");
	  argModel.addTest(new BasicSwimTest("Paddle", Arrays.asList("paddle")));
	  argModel.addTest(new BasicSwimTest("Complex Paddle", Arrays.asList("tadpole")));
      argModel.addTest(new BasicSwimTest("Human Crawl", Arrays.asList("humanCrawl")));
      argModel.addTest(new BasicSwimTest("Human Fly", Arrays.asList("humanFly")));
      argModel.addTest(new BasicSwimTest("(Rej Traj) Human Crawl", Arrays.asList("humanCrawlRefTraj")));
      argModel.addTest(new BasicSwimTest("(Rej Traj) Human Fly", Arrays.asList("humanFlyRefTraj")));
      argModel.addTest(new PaddleTest());
  }
}
