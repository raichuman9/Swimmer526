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
 * Created at 7:47:37 PM Jan 12, 2011
 */
package ubc.swim.tests;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.joints.RevoluteJoint;

import ubc.swim.dynamics.controllers.BuoyancyControllerDef;
import ubc.swim.dynamics.controllers.DynamicsController;
import ubc.swim.gui.SwimSettings;
import ubc.swim.world.Character;
import ubc.swim.world.HumanChar;
import ubc.swim.world.HumanChar.Stroke;

/**
 * Basic swimming test environment
 * 
 * Adapted from SliderCrankTest.java by Daniel Murphy
 * @author Ben Humberston
 *
 */
public class BasicSwimTest extends SwimTest {
	
	private RevoluteJoint m_joint1;
	
	private Character character;
	
	@Override
	public float getDefaultCameraScale() {
		return 100;
	}
	
	@Override
	public void initTest(boolean deserialized) {
		float fluidHeight = 10.0f;
		
		Body ground = null;
		{
			BodyDef bd = new BodyDef();
			ground = getWorld().createBody(bd);

			PolygonShape shape = new PolygonShape();
			shape.setAsEdge(new Vec2(-40.0f, 0.0f), new Vec2(40.0f, 0.0f));
			ground.createFixture(shape, 0.0f);
		}

//		{
//			Body prevBody = ground;
//
//			float boatLength = 6.0f;
//			float boatWidth = 0.5f;
//			float propLength = 2.0f;
//			float propWidth = 0.4f;
//			
//			// Define a powered boat :).
//			{
//				PolygonShape shape = new PolygonShape();
//				shape.setAsBox(boatLength, boatWidth);
//
//				BodyDef bd = new BodyDef();
//				bd.type = BodyType.DYNAMIC;
//				bd.position.set(0.0f, fluidHeight);
//				Body body = getWorld().createBody(bd);
//				body.createFixture(shape, 2.0f);
//
//				prevBody = body;
//			}
//
//			// Define follower.
//			{
//				PolygonShape shape = new PolygonShape();
//				shape.setAsBox(propWidth, propLength);
//
//				BodyDef bd = new BodyDef();
//				bd.type = BodyType.DYNAMIC;
//				bd.position.set(-boatLength, fluidHeight - propLength);
//				Body body = getWorld().createBody(bd);
//				body.createFixture(shape, 2.0f);
//
//				RevoluteJointDef rjd = new RevoluteJointDef();
//				rjd.initialize(prevBody, body, new Vec2(-boatLength, fluidHeight));
//				rjd.maxMotorTorque = 1000.0f;
//				rjd.motorSpeed = (float)(-2 * Math.PI);
//				rjd.enableMotor = true;
//				getWorld().createJoint(rjd);
//
//				prevBody = body;
//			}
//		}
		
		character = new HumanChar(Stroke.CRAWL);
		character.initialize(getWorld());
		character.moveTo(0, 10);
		
		//Create fluid environment
		BuoyancyControllerDef fluidDef = new BuoyancyControllerDef();
		fluidDef.density = 5.0f;
		fluidDef.offset = fluidHeight;
		fluidDef.linearDrag = 100.0f;
		fluidDef.useDensity = true;
		DynamicsController fluid = fluidDef.create();
		fluid.world = world;
		for (Body body = getWorld().getBodyList(); body != null; body = body.getNext()) {
			fluid.addBody(body);
		}
		addController(fluid);
	}
	
	@Override
	public void step(SwimSettings settings) {
		character.step(settings, getTimeStep(settings), runtime);
		
		super.step(settings);

	}

	@Override
	public void keyPressed(char argKeyChar, int argKeyCode) {
		
		switch(argKeyChar){
			case 'm':
				m_joint1.enableMotor(!m_joint1.isMotorEnabled());
				getModel().getKeys()['m'] = false;
				break;
		}
	}

	@Override
	public String getTestName() {
		return "Basic Swim Test";
	}

}
