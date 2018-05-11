import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.Map; 
import java.util.Set; 
import java.util.Arrays; 
import java.util.List; 
import java.util.LinkedList; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class ESEmulator extends PApplet {

 
 

 
 

//The number of charges in this animation. 
int num = 0;
//The bound of the velocity magnitude. 
int velocityBound = 50;
//The upper bound for the mass in kg. 
int massBound = 10;
//The upper bound for charges. 
int chargeBound = 10;
//The duration between frames. 
double duration;
//The masses of the particles. 
List<Float> mass = new ArrayList<Float>();
//The array of the values of the charges of the particles. 
List<Float> charges = new ArrayList<Float>();
//The float array of the coordinates of electric charges.
//The first index of the nested array is the x coordinate, the second the y. 
List<float[]> pos = new ArrayList<float[]>();
//The velocity of particles. 
List<float[]> velocity = new ArrayList<float[]>();
//The acceleration of the particles. 
List<float[]> acceleration = new ArrayList<float[]>();
//Whether the charge at the given index has collided with another. 
List<Boolean> state = new LinkedList<Boolean>(); 
//The charge that a given charge may have collided with. The charges are with different signs. 
Map<Integer,Integer> duoDif = new HashMap<Integer,Integer>(); 
//The diameter of each charge. 
int diameter;
//The diameter of a hydrogen atom. 
int orbitR; 
//Distance coefficient representing a ratio between the sizes of the screen and the actual distance between the charges. This ratio lies between 1 to 10. 
double distRatio = 20; 
//A counter that tracks the extent of motion in a period of orbiting particles. 
int counter = 0;
//A set of counters accounting for the time of presence of hydrogen atoms, who is defaulated to disappear after a duration of three second. 
Map<Integer,Integer> counters = new HashMap<Integer,Integer>(); 
//Decrease per frame of color for positive charges. 
float[] dePos = new float[3]; 
//Decrease per frame of color for negative charges. 
float[] deNeg = new float[3]; 
//The background color of the screen. 
int[] backColor = {95,231,247}; 
//The color of the positive charge. 
int[] posColor = {134,125,242}; 
//The color of the negative charge. 
int[] negColor = {255,0,0};
//A connector used for display purposes. 
Connector con;
//Holders of present state of the display--of the present charge and that to be next connected. 
int[] dispIndex = {0,1}; 
//Whether the connector is currently connecting two charges. 
boolean isConnect = false; 
//The number of seconds the formed hydrogen atoms fade. 
int fadeFactor = 15;  

public void setup()
{
  
  //Initilization. 
  for(int i=0; i<num; i++)
    populate();
  //Initialize the connector. 
  con = new Connector(); 
  frameRate(100); 
  //Initialize the dimensions of the charge and orbit. 
  diameter = width/50; 
  orbitR = width/25; 
  //Initialize the color ratios for fading.
  for(int i=0; i<3; i++)
  {
    dePos[i] = new Float(backColor[i]-posColor[i])/(fadeFactor*frameRate);
    deNeg[i] = new Float(backColor[i]-negColor[i])/(fadeFactor*frameRate); 
  }
  //Initialize the connector used for demonstration. 
  con = new Connector(); 
  duration = new Double(1)/frameRate;
}

public void draw()
{
  //Check for whether the process of calculation should be displayed by pressing the enter key. 
  if(keyPressed&&key==ENTER)
  {  
    //Check if the connection process is on going. 
    if(isConnect)
    {
      frameRate(5); 
      if(con.connect())
        return; 
      else 
        isConnect = false;
    }
    //Searching frame rate is unchanged. 
    frameRate(100); 
    //Shift of behavior to present different charges. 
    if(dispIndex[0]>=num)
      return;
    if(dispIndex[1]>=num)
    {
      //Increment the primary charge to the next. 
      dispIndex[0]++;
      dispIndex[1] = dispIndex[0]+1; 
      return; 
    }
    //If any of the two particles have either collided or fused, skip the demo. 
    if(state.get(dispIndex[0]))
    {
      //Increment to the next. 
      dispIndex[0]++; 
      return;
    }
    if(state.get(dispIndex[1]))
    {
      //Increment to the next. 
      dispIndex[1]++; 
      return;
    }
    //Set the connector to connect this two coordinates. 
    con.set((int)pos.get(dispIndex[0])[0],(int)pos.get(dispIndex[0])[1],(int)pos.get(dispIndex[1])[0],(int)pos.get(dispIndex[1])[1]); 
    //Indicate that two charges are currently being connected. 
    isConnect = true; 
    drawCharge(dispIndex[0]);
    drawCharge(dispIndex[1]); 
    //Increment the indices of the charges being displayed. 
    dispIndex[1]++;
    return; 
  }
  //Reset the demo conditions for future demos. 
  dispIndex[0] = 0;
  dispIndex[1] = 1; 
  //Prepare the standard settings for computations.  
  background(backColor[0],backColor[1],backColor[2]); 
  frameRate(100); 
  clearAcceleration(); 
  //Increment the counter for orbiting. 
  if(counter>frameRate) 
    counter = 0;
  counter++; 
  for(Integer center: duoDif.keySet())
    orbit(center,duoDif.get(center)); 
  //Compute the accelerations. 
  for(int i=0; i<num; i++)
  {
    if(state.get(i))
      continue;
    //Compute the acceleration of particles still in free motion. 
    for(int j=i+1; j<num; j++) 
    {
      if(state.get(j))
        continue; 
      computeAcceleration(i,j); 
    }
    //Increment the velocities and positions. 
    velocity.get(i)[0]+=acceleration.get(i)[0]*duration;
    velocity.get(i)[1]+=acceleration.get(i)[1]*duration;
    pos.get(i)[0]+=velocity.get(i)[0]*duration;
    pos.get(i)[1]+=velocity.get(i)[1]*duration;
    //Check if the particles are out of bounds. 
    if(pos.get(i)[0]-diameter<=0)
    {
      pos.get(i)[0] = diameter;
      velocity.get(i)[0] = abs(velocity.get(i)[0]);
    }
    if(pos.get(i)[0]+diameter>=width)
    {
      pos.get(i)[0] = width-diameter;
      velocity.get(i)[0] = -abs(velocity.get(i)[0]);
    }
    if(pos.get(i)[1]-diameter<=0)
    {
      pos.get(i)[1] = diameter;
      velocity.get(i)[1] = abs(velocity.get(i)[1]); 
    }
    if(pos.get(i)[1]+diameter>=height)
    {
      pos.get(i)[1] = height-diameter;
      velocity.get(i)[1] = -abs(velocity.get(i)[1]);
    }
    if(charges.get(i)>0)
      fill(255,0,0); 
    else
      fill(0,0,255);
    drawCharge(i); 
  }
  //This holder holds the current charges possible, irregardless of the charges that may
  //be engendered in the loop as they are not of effect at this frame. 
  int curNumHolder = num; 
  outer:
  for(int i=0; i<curNumHolder; i++)
  {
    if(state.get(i))
      continue; 
    for(int j=i+1; j<num; j++)
      if(isIn(j,pos.get(i)[0],pos.get(i)[1]))
      {
        if(state.get(j)) 
          continue; 
        //Set the states of the charges to post-collision. 
        state.set(i,true);
        state.set(j,true);
        //If the charges are of different signs. 
        if(charges.get(i)*charges.get(j)<0)
        {
          //Record the pair. 
         if(charges.get(i)>0)
           duoDif.put(i,j); 
         else
           duoDif.put(j,i); 
         counters.put(i,0);
         counters.put(j,0);
         continue outer;
        }
        //Else when two charges of the same change collide, assume they fuse and proceed in a random direction. 
        else
        {
          //Indicate fusion with a change of background color.  
          background(82,14,178);  
          populate(pos.get(i)[0],pos.get(j)[1],charges.get(i)+charges.get(j),mass.get(i)+mass.get(j));
          continue outer; 
        }
      } 
  }
}

//Compute the accelerations of two particles from their electrostatic interaction. 
public void computeAcceleration(int first, int second)
{
  //The distance between two particles. 
  double dist = Math.sqrt(Math.pow(pos.get(first)[0]-pos.get(second)[0],2)+Math.pow(pos.get(first)[1]-pos.get(second)[1],2))/distRatio; 
  //Magnitude of the force, with the discrepancy of the charges accounted. 
  float forceM = (float)(10*charges.get(first)*charges.get(second)/Math.pow(dist,2));
  //The projections of the force on the first particle. 
  double xPro = -forceM*(pos.get(second)[0]-pos.get(first)[0])/dist;
  double yPro = -forceM*(pos.get(second)[1]-pos.get(first)[1])/dist;
  acceleration.get(first)[0] = (float)xPro/mass.get(first);
  acceleration.get(first)[1] = (float)yPro/mass.get(first);
  acceleration.get(second)[0] = (float)-xPro/mass.get(second);
  acceleration.get(second)[1] = (float)-yPro/mass.get(second);
}

//Check if a given position is within the interior of a charge particle. 
public boolean isIn(int index, float xPos, float yPos)
{
  return xPos<pos.get(index)[0]+diameter&&xPos>pos.get(index)[0]-diameter&&yPos<pos.get(index)[1]+diameter&&yPos>pos.get(index)[1]-diameter; 
}

//Clear the acceleration of the charges. Should be invoked at each frame. 
public void clearAcceleration()
{
  for(float[] holder: acceleration)
  {
    holder[0] = 0.00f; 
    holder[1] = 0.00f; 
  }
}

//Populate a charge of random charge and mass at the given coordinate of the pointer. 
public void mouseClicked()
{
  populate(mouseX,mouseY); 
}

//Populate a particle at a specific coordinate with given mass and charge. 
public void populate(float x, float y, float chargeNet, float massNet)
{
  charges.add(chargeNet);
  mass.add(massNet); 
  float[] po = new float[2]; 
  po[0] = x; 
  po[1] = y; 
  float[] vel = new float[2]; 
  vel[0] = (Math.random()>0.5f?1:-1)*(int)(Math.random()*velocityBound);
  vel[1] = (Math.random()>0.5f?1:-1)*(int)(int)(Math.random()*velocityBound); 
  float[] acc = new float[2]; 
  acc[0] = 0;
  acc[1] = 0; 
  pos.add(po);
  velocity.add(vel); 
  acceleration.add(acc); 
  //Incerment the total count of charges. 
  num++; 
  state.add(false);  
}

//Populate a particle at a specific coordinate with random mass and charge generated. 
public void populate(float x, float y)
{
  charges.add((Math.random()<0.5f?1:-1)*3.00f);
  mass.add(random(massBound)); 
  float[] po = new float[2]; 
  po[0] = x; 
  po[1] = y; 
  float[] vel = new float[2]; 
  vel[0] = (Math.random()>0.5f?1:-1)*(int)(Math.random()*velocityBound);
  vel[1] = (Math.random()>0.5f?1:-1)*(int)(int)(Math.random()*velocityBound); 
  float[] acc = new float[2]; 
  acc[0] = 0;
  acc[1] = 0; 
  pos.add(po);
  velocity.add(vel); 
  acceleration.add(acc); 
  //Incerment the total count of charges. 
  num++; 
  state.add(false); 
}

//Populate a charge at a random coordinate. 
public void populate()
{
  populate(random(width),random(height));
}

//Depict a hydrogen atom. The center charge must be positive. 
public void orbit(int center, int orb)
{
  //If the atom ought to be already disappeared. 
  if(abs(posColor[0]+(counters.get(center)+1)*dePos[0]-backColor[0])<=1)
    return; 
  //Draw the orbit pass. 
  fill(backColor[0],backColor[1],backColor[2]); 
  ellipse(pos.get(center)[0],pos.get(center)[1],orbitR*2,orbitR*2); 
  //Draw the central charge. 
  drawCharge(center);  
  pos.get(orb)[0] = pos.get(center)[0]+(float)(orbitR*Math.cos(2*Math.PI*counter/new Float(frameRate)));
  pos.get(orb)[1] = pos.get(center)[1]+(float)(orbitR*Math.sin(2*Math.PI*counter/new Float(frameRate))); 
  //Draw the charge orbiting the central charge. 
  drawCharge(orb); 
  //Incerment the record of the time the atom has appeared. 
  counters.put(center,counters.get(center)+1);
  counters.put(orb,counters.get(orb)+1); 
}

//Display a charge based on its current position. 
public void drawCharge(int index)
{
  if(charges.get(index)>0)
  {
    fill(posColor[0],posColor[1],posColor[2]); 
    //If the charge being drawn is in a hydrogen atom. 
    if(state.get(index))
    {
      //If the atom ought to be already disappeared. 
      if(abs(posColor[0]+(counters.get(index)+1)*dePos[0]-backColor[0])<=5)
        return; 
      //Fading effect. 
      fill(posColor[0]+(counters.get(index)+1)*dePos[0],
           posColor[1]+(counters.get(index)+1)*dePos[1],
           posColor[2]+(counters.get(index)+1)*dePos[2]);
      //Increment the counter for the charge. 
      counters.put(index,counters.get(index)+1);
    }
    ellipse(pos.get(index)[0],pos.get(index)[1],diameter,diameter);
    line(pos.get(index)[0],pos.get(index)[1]-diameter/2,pos.get(index)[0],pos.get(index)[1]+diameter/2);
  }else
  {
    fill(negColor[0],negColor[1],negColor[2]); 
    if(state.get(index))
    {
       //If the atom ought to be already disappeared. 
      if(abs(posColor[0]+counters.get(index)+1*dePos[0]-backColor[0])<=5)
        return; 
      //Fading effect. 
      fill(negColor[0]+(counters.get(index)+1)*deNeg[0],
           negColor[1]+(counters.get(index)+1)*deNeg[1],
           negColor[2]+(counters.get(index)+1)*deNeg[2]);
      //Increment the counter for this charge. 
      counters.put(index,counters.get(index)+1); 
    }
    ellipse(pos.get(index)[0],pos.get(index)[1],diameter,diameter);
  }
  line(pos.get(index)[0]-diameter/2,pos.get(index)[1],pos.get(index)[0]+diameter/2,pos.get(index)[1]); 
}

/**
 * A connector of two points in which the two points are connected by segments. 
 */
class Connector
{
  //The current coordinate of the starting point. 
  private float curX, curY;
  //The ending coordinate of the line. 
  private float endX, endY; 
  //The increment in length of the line. 
  private float increX, increY; 
  //A counter of the number of segments drawn. This number should not exceed 10. 
  private int counter; 
  
  public Connector(int startX, int startY, int endX, int endY)
  {
    set(startX,startY,endX,endY); 
  }
  
  //A constructor reserved for general use in which at the time of construction the 
  //starting and ending coordinates are not known, and therefore the set method must 
  //be explicitly invoked preceding drawing the line segments. 
  protected Connector()
  {
    
  }
  
  //Draw a segment of the increment from the current position to the ending point. 
  public boolean connect()
  {
    if(counter>4)
      return false; 
    line(curX,curY,curX+increX,curY+increY);
    curX+=2*increX; 
    curY+=2*increY;
    counter++; 
    return true; 
  }
  
  //Reset the connector to a different set of starting and ending points. 
  public void set(int startX, int startY, int endX, int endY)
  {
    //Reset the counter. 
    counter = 0; 
    //Reset the positions. 
    curX = startX;
    curY = startY; 
    this.endX = endX;
    this.endY = endY; 
    //Calculate the increment based on the distance of between the points. 
    double dist = Math.sqrt(Math.pow(endX-startX,2)+Math.pow(endY-startY,2));
    float incre = new Float(dist/10);  
    increX = new Float(incre*(this.endX-curX)/dist); 
    increY = new Float(incre*(this.endY-curY)/dist); 
  }
  
  public String toString()
  {
    return String.format("(%s, %s) to (%s, %s)",curX,curY,endX,endY);
  }
  
}
  public void settings() {  fullScreen(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--present", "--window-color=#666666", "--stop-color=#cccccc", "ESEmulator" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
