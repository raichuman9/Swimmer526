package ubc.swim.world.characters;

import java.util.ArrayList;

import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Filter;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jbox2d.pooling.arrays.Vec2Array;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubc.swim.gui.SwimSettings;
import ubc.swim.world.trajectory.PolynomialTrajectory;
import ubc.swim.world.trajectory.RefTrajectory;
import ubc.swim.world.trajectory.SineTrajectory;

/**
 * A roughly humanoid swimmer that uses PD-controllers with optimized reference trajectories
 * @author Ben Humberston
 *
 */
public class RefTrajHumanChar extends SwimCharacter {
	private static final Logger log = LoggerFactory.getLogger(RefTrajHumanChar.class);
	
	protected static final float TWO_PI = (float)(2 * Math.PI);
	
	protected static final int NUM_SINE_TRAJECTORIES_PER_SIDE = 3; //one each for elbows, hips, and knees (shoulders use poly trajectory)
	
	protected static final int NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY = 2; //increase or decrease to control complexity
	protected static final int NUM_PARAMS_PER_SINE_BASIS_FUNC = 3;	 	//amplitude (weight), phase offset, and period 
	protected static final int NUM_PARAMS_PER_SINE_TRAJECTORY = NUM_PARAMS_PER_SINE_BASIS_FUNC * NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY;
	
	protected static final int NUM_BASIS_FUNCS_PER_SHOULDER_TRAJECTORY = 2; //increase or decrease to control complexity 
	protected static final int NUM_PARAMS_PER_SHOULDER_BASIS_FUNC = 2; 		//coefficient & exponent 
	protected static final int NUM_PARAMS_PER_SHOULDER_TRAJECTORY = NUM_PARAMS_PER_SHOULDER_BASIS_FUNC * NUM_BASIS_FUNCS_PER_SHOULDER_TRAJECTORY;
	
	protected static final float MIN_STROKE_PERIOD = 0.2f; 
	
	//Body params
	protected float height = 2; //2 meters
	protected float headHeight = height / 8;
	protected float headWidth = headHeight * 0.85f;
	protected float armLen = height * 0.45f;
	protected float upperArmLen = armLen * (1 / 2.2f);
	protected float upperArmWidth = height / 16;
	protected float lowerArmLen = armLen - upperArmLen;
	protected float lowerArmWidth = upperArmWidth * 0.85f;
	protected float legLen = height * 0.54f;
	protected float upperLegLen = legLen * 0.46f;
	protected float upperLegWidth = height / 10;
	protected float lowerLegLen = legLen - upperLegLen;
	protected float lowerLegWidth = upperLegWidth * 0.7f;
	protected float footLen = height * 0.035f;
	
	protected Stroke stroke;
	
	protected float shoulderPeriod; //time period over which shoulders go through full rotation
	
	protected ArrayList<Body> rightBodies;
	protected ArrayList<Body> leftBodies;
	
	protected ArrayList<Joint> joints;
	protected ArrayList<Joint> leftJoints;
	protected ArrayList<Joint> rightJoints;
	
	protected float prevTorque = 0.0f;
	
	protected ArrayList<RefTrajectory> trajectories;
	protected ArrayList<SineTrajectory> sineTrajectories;
	protected ArrayList<PolynomialTrajectory> shoulderTrajectories;
	
	private final Vec2Array tlvertices = new Vec2Array(); //used for debug drawing
	
	/**
	 * Create a new human character with given stroke
	 * shoulderPeriod specifies the time interval over which shoulder joints try to 
	 * execute a full rotation.
	 * @param stroke
	 */
	public RefTrajHumanChar(Stroke stroke, float shoulderPeriod) {
		super();
		
		this.stroke = stroke;
		this.shoulderPeriod = shoulderPeriod;
	
		rightBodies = new ArrayList<Body>();
		leftBodies = new ArrayList<Body>();
		
		joints = new ArrayList<Joint>();
		
		leftJoints = new ArrayList<Joint>();
		rightJoints = new ArrayList<Joint>();
		
		trajectories = new ArrayList<RefTrajectory>();
		sineTrajectories = new ArrayList<SineTrajectory>();
		shoulderTrajectories = new ArrayList<PolynomialTrajectory>();
	}

	@Override
	public int getNumControlDimensions() {
		//NOTE: shoulder trajectory is manually coded and not included in control dims
		
		int numJoints = 1; //1 for head
		numJoints += 1; //elbow 
		numJoints += 2; //hip & knee
		return numJoints * NUM_PARAMS_PER_SINE_TRAJECTORY;
	}
	
	@Override
	public void initialize(World world) {
		float defaultDensity = 1.1f;
		float torsoDensity = 0.5f;
		
		float torsoHeight = height - legLen - headHeight;
		float torsoWidth = height / 8;
		
		//Create the main torso and head
		Body torso;
		{
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(torsoHeight/2, torsoWidth/2);
	
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			torso = world.createBody(bd);
			torso.createFixture(shape, torsoDensity);
			bodies.add(torso);
			
			rootBody = torso;
		}
		
		Body head;
		{
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(headHeight/2, headHeight/2);
	
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			bd.position.set(torsoHeight/2 + headHeight/2, 0.0f);
			head = world.createBody(bd);
			head.createFixture(shape, defaultDensity);
			
			RevoluteJointDef rjd = new RevoluteJointDef();
			rjd.initialize(torso, head, new Vec2(torsoHeight/2, 0.0f));
			rjd.enableLimit = true;
			rjd.upperAngle = (float)Math.PI/10;
			rjd.lowerAngle = (float)-Math.PI/10;
			RevoluteJoint neckJoint = (RevoluteJoint)(world.createJoint(rjd));
			joints.add(neckJoint);
			SineTrajectory neckTrajectory = new SineTrajectory(NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY);
			neckTrajectory.setJoint(neckJoint);
			sineTrajectories.add(neckTrajectory);
			
			bodies.add(head);
		}

		//Create arms and legs for left and right sides
		//NOTE: bit of a mess and could be simplified a good deal, but it works...
		Vec2 armJointPoint = new Vec2(torsoHeight/2, 0.0f);
		Vec2 legJointPoint = new Vec2(-torsoHeight/2, 0.0f);
		ArrayList<Joint> sideJoints = null;
		for (int i = 0; i < 2; i++) {
			PolygonShape shape = new PolygonShape();
			shape.setAsBox(upperArmLen/2, upperArmWidth/2);
			
			sideJoints = (i == 0) ? rightJoints : leftJoints;
	
			//Upper arm
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//Start second arm lifted above head for crawl
			float upperArmOffset = 0, upperArmRot = 0;
			if (i == 1 && stroke == Stroke.CRAWL) {
				upperArmOffset = -upperArmLen/2;
				upperArmRot = 0;
			}
			else {
				upperArmOffset = upperArmLen/2;
				upperArmRot = (float)Math.PI;
			}
			bd.position.set(armJointPoint.x + upperArmOffset, armJointPoint.y);
			bd.angle = upperArmRot;
			Body upperArm = world.createBody(bd);
			upperArm.createFixture(shape, defaultDensity);
			
			RevoluteJointDef rjd = new RevoluteJointDef();
			rjd.initialize(torso, upperArm, new Vec2(armJointPoint.x, armJointPoint.y));
			RevoluteJoint shoulderJoint = (RevoluteJoint)world.createJoint(rjd);
			sideJoints.add(shoulderJoint);
			PolynomialTrajectory shoulderTraj = new PolynomialTrajectory();
			shoulderTraj.setJoint(shoulderJoint);
			//Manually-defined linear trajectory for shoulder through a full rotation
			float shoulderFuncSlope = -2 * (float)Math.PI / shoulderPeriod;
			for (int j = 0; j < NUM_BASIS_FUNCS_PER_SHOULDER_TRAJECTORY; j++) {
				if (j == 1) //t^1 term
					shoulderTraj.setTermCoefficient(j, shoulderFuncSlope);
				else //all other terms set to zero for now
					shoulderTraj.setTermCoefficient(j, 0); 
			}
			shoulderTrajectories.add(shoulderTraj);
			
			bodies.add(upperArm);
			
			//Lower Arm
			shape = new PolygonShape();
			shape.setAsBox(lowerArmLen/2, lowerArmWidth/2);
			bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//Start second arm lifted above head for crawl
			float lowerArmOffset = 0, lowerArmRot = 0;
			float minElbowAngle = 0, maxElbowAngle = 0;
			if (i == 1 && stroke == Stroke.CRAWL) {
				lowerArmOffset = -upperArmLen/2 - lowerArmLen/2;
				lowerArmRot = 0;
				minElbowAngle = -(float)Math.PI * 0.1f;
				maxElbowAngle = (float)Math.PI * 0.1f;
			}
			else {
				lowerArmOffset = upperArmLen/2 + lowerArmLen/2;
				lowerArmRot = (float)Math.PI;
				minElbowAngle = (float)-Math.PI * 0.1f;
				maxElbowAngle = (float)Math.PI * 0.1f;
			}
			bd.position.set(upperArm.getPosition().x + lowerArmOffset, 0.0f);
			bd.angle = lowerArmRot;
			Body lowerArm = world.createBody(bd);
			lowerArm.createFixture(shape, defaultDensity);
			
			//Elbow joint
			{
				rjd = new RevoluteJointDef();
				rjd.enableLimit = true;
				rjd.upperAngle = maxElbowAngle;
				rjd.lowerAngle = minElbowAngle;
				rjd.initialize(upperArm, lowerArm, new Vec2(0.5f * (upperArm.getPosition().x + lowerArm.getPosition().x), 0.5f * (upperArm.getPosition().y + lowerArm.getPosition().y)));
				sideJoints.add(world.createJoint(rjd));
				RevoluteJoint elbowJoint = (RevoluteJoint)(world.createJoint(rjd));
				sideJoints.add(elbowJoint);
				SineTrajectory elbowTrajectory = new SineTrajectory(NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY);
				elbowTrajectory.setJoint(elbowJoint);
				sineTrajectories.add(elbowTrajectory);
			}
			
			bodies.add(lowerArm);
			
			//Upper leg
			shape = new PolygonShape();
			shape.setAsBox(upperLegLen/2, upperLegWidth/2);
			bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//TODO: start in/out of phase for fly/crawl (constant for now)
			float upperLegOffset = (i == 1 && stroke == Stroke.CRAWL) ? -upperLegLen/2 : -upperLegLen/2;
			bd.position.set(legJointPoint.x + upperLegOffset, legJointPoint.y);
			Body upperLeg = world.createBody(bd);
			upperLeg.createFixture(shape, defaultDensity);
			
			//Hip joint
			{
				rjd = new RevoluteJointDef();
				rjd.enableLimit = true;
				rjd.upperAngle = (float)Math.PI / 4;
				rjd.lowerAngle = (float)-Math.PI / 4;
				rjd.initialize(torso, upperLeg, new Vec2(legJointPoint.x, legJointPoint.y));
				sideJoints.add(world.createJoint(rjd));
				RevoluteJoint hipJoint = (RevoluteJoint)(world.createJoint(rjd));
				sideJoints.add(hipJoint);
				SineTrajectory hipTrajectory = new SineTrajectory(NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY);
				hipTrajectory.setJoint(hipJoint);
				sineTrajectories.add(hipTrajectory);
			}
			
			bodies.add(upperLeg);
			
			//Lower leg
			shape = new PolygonShape();
			shape.setAsBox(lowerLegLen/2, lowerLegWidth/2);
			bd = new BodyDef();
			bd.type = BodyType.DYNAMIC;
			//TODO: lower leg offset that varies by stroke (constant for now)
			float lowerLegOffset = (i == 1 && stroke == Stroke.CRAWL) ? -upperLegLen/2 - lowerLegLen/2 : -upperLegLen/2 - lowerLegLen/2;
			bd.position.set(upperLeg.getPosition().x + lowerLegOffset, 0.0f);
			Body lowerLeg = world.createBody(bd);
			lowerLeg.createFixture(shape, defaultDensity);
			
			//Knee joint
			{
				rjd = new RevoluteJointDef();
				rjd.enableLimit = true;
				rjd.upperAngle = (float)0;
				rjd.lowerAngle = (float)-Math.PI * 0.9f;
				rjd.initialize(upperLeg, lowerLeg, new Vec2(0.5f * (upperLeg.getPosition().x + lowerLeg.getPosition().x), 0.5f * (upperLeg.getPosition().y + lowerLeg.getPosition().y)));
				sideJoints.add(world.createJoint(rjd));
				RevoluteJoint kneeJoint = (RevoluteJoint)(world.createJoint(rjd));
				sideJoints.add(kneeJoint);
				SineTrajectory kneeTrajectory = new SineTrajectory(NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY);
				kneeTrajectory.setJoint(kneeJoint);
				sineTrajectories.add(kneeTrajectory);
			}
			
			bodies.add(lowerLeg);
			
			if (i == 0) { 
				rightBodies.add(upperArm); 	rightBodies.add(lowerArm);
				rightBodies.add(upperLeg); 	rightBodies.add(lowerLeg); 
			}
			else  { 
				leftBodies.add(upperArm); 	leftBodies.add(lowerArm);
				leftBodies.add(upperLeg); 	leftBodies.add(lowerLeg);
			}
			
			joints.addAll(sideJoints);
		}
		
		trajectories.addAll(sineTrajectories);
		trajectories.addAll(shoulderTrajectories);
		
		//Set all bodies to be non-colliding with each other
		Filter filter = new Filter();
		filter.groupIndex = -1;
		for (int i = 0; i < bodies.size(); i++) {
			Body body = bodies.get(i);
			body.getFixtureList().setFilterData(filter);
		}
	}
	
	@Override
	public void setControlParams(double[] params) {
		if (params.length != getNumControlDimensions()) {
			log.error("Character expected control params of size " + getNumControlDimensions() + " but was given params of size " + params.length);
			assert(false);
		}

		this.controlParams = params;
		
		//Trajectory order:
		// Sine trajectories
		//    neck
		//    For left & right sides (split by side)
		//	    elbows
		//	    hips
		//      knees
		// Poly trajectories
		//    shoulders (note: manually controlled; input params do not modify these trajectories)
		
		//Update sine-based reference trajectory params
		int leftSideParamsStartIdx = 1 + (sineTrajectories.size() - 1) / 2; //left hand controls start in second half of sublist once neck is removed
		for (int i = 0; i < sineTrajectories.size(); i++) {
			boolean isLeftSideTraj = i >= leftSideParamsStartIdx;
			
			SineTrajectory trajectory = (SineTrajectory)sineTrajectories.get(i);
			
			int paramsIdx = i * NUM_PARAMS_PER_SINE_TRAJECTORY;
			//Mirror right side controls onto corresponding left side joints
			if (isLeftSideTraj) 
				paramsIdx -= NUM_SINE_TRAJECTORIES_PER_SIDE * NUM_PARAMS_PER_SINE_TRAJECTORY;
			
			//Set vals for each basis function of the trajectory
			for (int j = 0; j < NUM_BASIS_FUNCS_PER_SINE_TRAJECTORY; j++) {
				int paramsIdxOffset = j * NUM_PARAMS_PER_SINE_BASIS_FUNC;
				
				float weight = 		(float)params[paramsIdx + paramsIdxOffset];
				float period = 		MIN_STROKE_PERIOD + Math.abs((float)params[paramsIdx + paramsIdxOffset + 1]);	//TODO: try fixed period?	
				float phaseOffset = (float)params[paramsIdx + paramsIdxOffset + 2];
				
				//If using crawl stroke, add additional 180 degree phase offset to left side vs. right side
				if (stroke == Stroke.CRAWL && j > leftSideParamsStartIdx)
					phaseOffset += (float) Math.PI;
				
				trajectory.setSineParams(j, weight, period, phaseOffset);
			}
		}
		
	}
	
	@Override
	public void step(SwimSettings settings, float dt, float runtime) {
		if (dt == 0) return;
		
		prevTorque = 0.0f;
		
		final float PD_GAIN = 1.0f;
		final float PD_DAMPING = 0.05f;
		
		//TODO: use time to drive right shoulder, but use right shoulder phase 
		//to drive other trajectories (requires tweaking left should trajectory period)
		
		//Update shoulder trajectories
		//TODO: pretty much same update as for other trajs; merge?
		for (int i = 0; i < shoulderTrajectories.size(); i++) {
			RefTrajectory trajectory = shoulderTrajectories.get(i);
			RevoluteJoint joint = trajectory.getJoint();
			
			float jointAngle = joint.getJointAngle() % TWO_PI;
			float jointSpeed = joint.getJointSpeed();
			
			float targAngle = trajectory.getValue(runtime, dt) % TWO_PI;
			
			//PD controller
			float torque = -PD_GAIN * (jointAngle - targAngle) - PD_DAMPING * jointSpeed;
			
			joint.getBodyA().applyTorque(torque);
			joint.getBodyB().applyTorque(torque);
			
			prevTorque += torque;
		}
		
		//Update the other trajectories (neck, elbows, knees, hips)
		for (int i = 0; i < sineTrajectories.size(); i++) {
			RefTrajectory trajectory = sineTrajectories.get(i);
			RevoluteJoint joint = trajectory.getJoint();
			
			float jointAngle = joint.getJointAngle() % TWO_PI;
			float jointSpeed = joint.getJointSpeed();
			
			float targAngle = trajectory.getValue(runtime, dt) % TWO_PI;
			
			//PD controller
			float torque = -PD_GAIN * (jointAngle - targAngle) - PD_DAMPING * jointSpeed;
			
			joint.getBodyA().applyTorque(torque);
			joint.getBodyB().applyTorque(torque);
			
			prevTorque += torque;
		}
	}
	
	@Override
	public float getPrevTorque() {
		return prevTorque;
	}
	
	@Override
	public void debugDraw(DebugDraw debugDraw) {
		Transform transform = new Transform();
		Color3f color = new Color3f();
		
		//Draw left/right coloring (note: left is drawn below right side... it's on the character side *away* from the viewer)
		for (Body body : leftBodies) {
			transform.set(body.getTransform());
			color.set(0.2f, 0.9f, 0.2f);
			for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext())
				drawPolygon((PolygonShape) fixture.getShape(), transform, color, debugDraw);
		}
		for (Body body : rightBodies) {
			transform.set(body.getTransform());
			color.set(0.9f, 0.2f, 0.2f);
			for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext())
				drawPolygon((PolygonShape) fixture.getShape(), transform, color, debugDraw);
		}
	}
	
	protected void drawPolygon(PolygonShape shape, Transform transform, Color3f color, DebugDraw debugDraw) {
		int vertexCount = shape.m_vertexCount;
		assert (vertexCount <= Settings.maxPolygonVertices);
		Vec2[] vertices = tlvertices.get(Settings.maxPolygonVertices);
		
		for (int i = 0; i < vertexCount; ++i) {
			Transform.mulToOut(transform, shape.m_vertices[i], vertices[i]);
		}
		
		debugDraw.drawSolidPolygon(vertices, vertexCount, color);
	}
}