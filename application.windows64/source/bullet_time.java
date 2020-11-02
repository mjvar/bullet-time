import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.sound.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class bullet_time extends PApplet {


// Sound files for game audio
SoundFile gameMusic;
SoundFile deathSound;
SoundFile winSound;
SoundFile menuMusic;

// Menu/screen stuff
PImage[] deathScreens = new PImage[4];
PImage[] winScreens = new PImage[4];
PImage startScreen;
PImage bg;
PImage start;
PImage levels;

// Ship assets
PImage base;
PImage u;
PImage d;
PImage l;
PImage r;
PImage ul;
PImage ur;
PImage dl;
PImage dr;

// Other pics
PImage shooty;
PImage portal;

// Level icons
PImage[] lvs = new PImage[12];

// WHY DOES PROCESSING NOT HAVE THIS BUILT IN
// They already did it for mousePressed !!!!!
boolean mouseReleased = false;

// Music control things
boolean deathSoundPlayed = false;
boolean winSoundPlayed = false;
boolean menuMusicPlayed = false;
float musicRate = 1;

// Global var for movement
float currentSpeed = 3; 

int shipDiam = 40;

// Game control things
int currentLevel = 0;
boolean gameOn = false;
boolean levelWin = false;
boolean levelSetup = false;
boolean slowOn = false;

// Frame iterator for win/death screens
int fIt = 0;

public void setup() {
  
    
  // Loading game sounds
  gameMusic = new SoundFile(this, "sounds/game-music.mp3");
  deathSound = new SoundFile(this, "sounds/death-sound.mp3");
  winSound = new SoundFile(this, "sounds/win-sound.mp3");
  menuMusic = new SoundFile(this, "sounds/menu-music.mp3");
  
  // Loading ship sprites
  base = loadImage("images/base.png");
  u = loadImage("images/u.png");
  ul = loadImage("images/ul.png");
  ur = loadImage("images/ur.png");
  d = loadImage("images/d.png");
  dl = loadImage("images/dl.png");
  dr = loadImage("images/dr.png");
  l = loadImage("images/l.png");
  r = loadImage("images/r.png");
  
  shooty = loadImage("images/shooty.png");
  portal = loadImage("images/portal.png");
  
  // Loading win/death screens
  for(int i = 0; i <= 3; i++){
    deathScreens[i] = loadImage("images/death" + str(i) + ".png");
    winScreens[i] = loadImage("images/win" + str(i) + ".png");
  }
  
  // Loading other menu assets
  startScreen = loadImage("images/startscreen.png");
  bg = loadImage("images/bg.png");
  start = loadImage("images/start.png");
  levels = loadImage("images/levels.png");
  
  // Loading level icons
  for(int i = 0; i < 12; i++){
    lvs[i] = loadImage("images/lv" + str(i+1) + ".png");
  }
  
  // Play that funky music
  menuMusic.loop();
}      

public void draw() {
  // Setting rate of music
  gameMusic.rate(musicRate);
  
  // Run the game
  runGame();
}

public void keyPressed(){
  // For slow stuff
  if(key == ' '){
    if(slowMeter > 0){
      slowOn = true;
    }
  }
  else {
    myShip.setMove(keyCode, true);
  }
}

public void keyReleased(){
  // For slow stuff
  if(key == ' '){
    slowOn = false;
  }
  else {
    myShip.setMove(keyCode, false);
  }
}

public void mouseClicked(){
  mouseReleased = true;
}
// global vars for entities
ArrayList<Bullet> bullets = new ArrayList<Bullet>();
ArrayList<Shooter> shooters = new ArrayList<Shooter>();
Ship myShip = new Ship(shipDiam, new PVector(shipDiam, height/2));
Portal myPortal;

int slowMeter = 200;

class Ship{
  PVector loc = new PVector();
  int diam;  
  
  boolean isUp, isLeft, isRight, isDown;
  
  Ship(int d, PVector l){
    // Set ship diameter
    diam = d;
    loc = l.copy();
  }
  
  public void displayShip(){
    float imgScl = 1.4f;
    imageMode(CENTER);
    if(isUp){
      if(isLeft){
        image(dr, loc.x, loc.y, diam*imgScl, diam*imgScl);
      }
      else if(isRight){
        image(dl, loc.x, loc.y, diam*imgScl, diam*imgScl);
      }
      else{
        image(d, loc.x, loc.y, diam*imgScl, diam*imgScl);
      }
    }
    else if(isDown){
      if(isLeft){
        image(ur, loc.x, loc.y, diam*imgScl, diam*imgScl);
      }
      else if(isRight){
        image(ul, loc.x, loc.y, diam*imgScl, diam*imgScl);
      }
      else{
        image(u, loc.x, loc.y, diam*imgScl, diam*imgScl);
      }
    }
    else if(isLeft){
      image(r, loc.x, loc.y, diam*imgScl, diam*imgScl);
    }
    else if(isRight){
      image(l, loc.x, loc.y, diam*imgScl, diam*imgScl);
    }
    else{
      image(base, loc.x, loc.y, diam*imgScl, diam*imgScl);
    }
    // This is the hitbox of the ship, in case I want to see it
    //fill(255);
    //ellipse(loc.x, loc.y, diam, diam);
  }
  
  public boolean setMove(int k, boolean b){
    // Nice, smooth, spicy diagonal movement
    // WHY WAS THIS SO HARD IN PROCESSING
    switch(k){
      case +'W':
      case UP:
        return isUp = b;
   
      case +'S':
      case DOWN:
        return isDown = b;
   
      case +'A':
      case LEFT:
        return isLeft = b;
   
      case +'D':
      case RIGHT:
        return isRight = b;
      default:
        return b;
    }
  }
  
  public void moveShip(){
    // Move ship based on current active directions, constrained to canvas
    int r = diam >> 1;
    loc.x = constrain(loc.x + currentSpeed*(PApplet.parseInt(isRight) - PApplet.parseInt(isLeft)), r, width  - r);
    loc.y = constrain(loc.y + currentSpeed*(PApplet.parseInt(isDown)  - PApplet.parseInt(isUp)),   r, height - r);
  }
}

class Shooter{
  PVector loc;
  PVector dir;
  
  // Frequency of shooting (important for Berserker)
  int freq;
  
  // SHOOTER TYPES:
  // 0 - regular, shoots at set direction
  // 1 - sniper, shoots in player's direction
  // 2 - berserker, rotates around rapidly, shooting in all directions
  int type;
  
  // Theta value for rotations for berserker
  float theta = 0;
  
  Shooter(int t, PVector l, int f, PVector d){
    // Constructor for Regular, which takes a set direction
    loc = l.copy();
    dir = d.copy();    
    freq = f;
    type = t;
  }
  
  Shooter(int t, PVector l, int f){
    // Constructor for Berserker and Sniper, which don't take a set direction
    loc = l.copy();
    dir = new PVector(0,0);   
    freq = f;
    type = t;
  }
  
  public void shootBullet(){
    theta += 0.1f;
    if(frameCount % (freq/currentSpeed) == 0){
      if(type == 0){
        // REGULAR - just shoot in provided direction
        bullets.add(new Bullet(loc, dir));
      }
      else if(type == 1){
        // SNIPER - shoot in direction of player
        PVector d = PVector.sub(myShip.loc, loc);
        bullets.add(new Bullet(loc, d.normalize()));
      }
      else if(type == 2){
        // BERSERKER - shoot in a spiral pattern
        PVector d = new PVector(cos(theta),sin(theta));
        bullets.add(new Bullet(loc, d));
      }
    }
  }
  
  public void displayShooter(){
    fill(255);
    imageMode(CENTER);
    image(shooty, loc.x, loc.y);
    //ellipse(loc.x, loc.y, 100, 100);
  }
}

class Bullet{
  PVector loc;
  PVector vel;
  
  Bullet(PVector l, PVector v){
    loc = l.copy();
    vel = v.copy();
  }
  
  public void updateBullet(){
    // Move bullet based on current game speed
    loc.add(PVector.mult(vel, currentSpeed));
  }
  
  public void displayBullet() {
    // Display bullet with a trail
    for(int i = 0; i < 7; i++){
      fill(255, 153, 20, 160);
      noStroke();
      ellipse(loc.x - vel.x*(i+1)*3, loc.y - vel.y*(i+1)*3, 8-i, 8-i);
    }
    fill(255);
    rectMode(CENTER);
    rect(loc.x, loc.y, 8, 8);
  }
}

class Portal{
  // End goal for any given level
  PVector loc;
  
  Portal(PVector l){
    loc = l.copy();
  }
  
  public void displayPortal(){
    fill(143, 232, 65);
    image(portal, loc.x, loc.y, shipDiam*1.4f, shipDiam*1.4f);
    //ellipse(loc.x, loc.y, shipDiam, shipDiam);
  }
}
public void runGame(){
  // If a level is active
  if(gameOn){
    imageMode(CENTER);
    image(bg, width/2,height/2);
    // Move ship
    myShip.moveShip();
    
    // Move and show bullets
    for(Bullet b : bullets){
      b.updateBullet();
      b.displayBullet();
    }
    
    // Shoot bullets and show shooters
    for(Shooter s : shooters){
      s.shootBullet();
      s.displayShooter();
    }
    
    // Show ship and portal 
    myShip.displayShip();
    myPortal.displayPortal();
    
    // Clean up dead bullets
    cleanBullets();
    
    // Check for win/loss conditions
    checkWin();
    checkDeath();
    
    slowMechanics();
    showMeter();
  }
  // If a level has yet to be set up
  else if(!levelSetup){
    // set up the level (or menu) based on current level
    switch(currentLevel){
      case -1:
        levelScreen();
        break;
      case 0:
        startScreen();
        break;
      case 1:
        level1();
        break;
      case 2:
        level2();
        break;
      case 3:
        level3();
        break;
      case 4:
        level4();
        break;
      case 5:
        level5();
        break;
      case 6:
        level6();
        break;
      case 7:
        level7();
        break;
      case 8:
        level8();
        break;
      case 9:
        level9();
        break;
      case 10:
        level10();
        break;
      case 11:
        level11();
        break;
      case 12:
        level12();
        break;
      default:
        // Handler case for bugs (of which I hope there are none)
        currentLevel = 0;
        break;
    }
  }
  // If the game has been won
  else if(levelWin){
    winScreen();
    gameMusic.stop();
    if(!winSoundPlayed){
      winSound.play();
      winSoundPlayed = true;
    }
    if(keyPressed){
      // Continue to next level
      if(key == 'c'){
        levelSetup = false;
        currentLevel++;
      }
      // Or go to the menu
      else if(key == 'm'){
        menuMusicPlayed = false;
        levelSetup = false;
        currentLevel = 0;
      }
    }
  }
  // If the player died
  else{
    deathScreen();
    gameMusic.stop();
    if(!deathSoundPlayed){
      deathSound.play();
      deathSoundPlayed = true;
    }
    if(keyPressed){
      // Restart level
      if(key == 'r'){
        levelSetup = false;
      }
      // Or go to the menu
      else if(key == 'm'){
        menuMusicPlayed = false;
        levelSetup = false;
        currentLevel = 0;
      }
    }
  }
  mouseReleased = false;
}

public void checkWin(){
  // Check if player reached portal
  if(dist(myPortal.loc.x, myPortal.loc.y, myShip.loc.x, myShip.loc.y) <= shipDiam){
    gameOn = false;
    levelWin = true;
  }
}

public void checkDeath(){
  // Check if player has hit any active bullets
  for(Bullet b : bullets){
    if(dist(b.loc.x, b.loc.y, myShip.loc.x, myShip.loc.y) <= shipDiam/2){
      gameOn = false;
      fill(0);
    }
  }
}

public void cleanBullets(){
  // Delete inactive bullets to avoid big stinky lag (IMPORTANT!!)
  for(int i = 0; i < bullets.size() - 1; i++){
    if(bullets.get(i).loc.x < -20 || bullets.get(i).loc.x > width + 20 || bullets.get(i).loc.y < -20 || bullets.get(i).loc.y > height + 20){
      bullets.remove(i);
    }
  }
}

public void globalResets(){  
  // These things reset every time a level ends,
  // so the program can call them all at once here
  deathSoundPlayed = false;
  winSoundPlayed = false;
  gameOn = true;
  levelSetup = true;
  levelWin = false;
  deathSound.stop();
  winSound.stop();
  gameMusic.loop();
  menuMusic.stop();
  
  slowMeter = 200;
}

public void winScreen(){
  // Show the win graphic
  pushMatrix();
  imageMode(CENTER);
  image(winScreens[fIt%4], width/2, height/2);
  popMatrix();
  // animating it
  if(frameCount%20 == 0){
    fIt++;
  }
}

public void deathScreen(){
  // Show the win graphic
  pushMatrix();
  imageMode(CENTER);
  image(deathScreens[fIt%4], width/2, height/2);
  popMatrix();
  // animating it
  if(frameCount%20 == 0){
    fIt++;
  }
}

public void showMeter(){
  // Show the slow meter in the top left
  pushMatrix();
  rectMode(CORNER);
  fill(255);
  if(slowMeter < 10){
    // If you're low on meter, this is bad!! I should warn you by turning red!!!
    fill(255,0,0);
  }
  stroke(0);
  strokeWeight(5);
  // Size changes based on meter amount
  rect(width*0.01f, height*0.01f, map(slowMeter, 0, 200, width*0.01f, width*0.1f), height*0.03f);
  popMatrix();
}

public void slowMechanics(){
  // Implement slow mechanics
  if(slowOn && slowMeter > 0){
    // If space is pressed AND meter not empty, do the slow stuff
    slowMeter--;
    musicRate = 0.8f;
    currentSpeed = 0.5f;
  }
  else{
    if(!slowOn && slowMeter < 200){
      // If space is NOT pressed AND meter is not full, fill it
      // Otherwise, user has to release space to fill the meter
      slowMeter++;
    }
    musicRate = 1;
    currentSpeed = 3;
  }
}

public void levelScreen(){
  imageMode(CENTER);
  image(bg, width/2, height/2);
  
  // Print all the level icons
  for(int x = 0; x < 4; x++){
    for(int y = 0; y < 3; y++){
      imageMode(CENTER);
      if(mouseX > (1+x)*width/5 - 50 && mouseX < (1+x)*width/5 + 50 && mouseY > (1+y)*height/4 - 75 && mouseY < (1+y)*height/4 + 75){
        // Expand a level icon when moused over
        image(lvs[x + y*4], (1+x)*width/5, (1+y)*height/4, 110, 165);
        if(mouseReleased){
            currentLevel = (x + y*4) + 1;
        }
      }
      else{
        image(lvs[x + y*4], (1+x)*width/5, (1+y)*height/4);
      }
    }
  }
}

public void startScreen(){
  deathSound.stop();
  winSound.stop();
  
  if(!menuMusicPlayed){
    menuMusicPlayed = true;
    menuMusic.loop();
  }
  imageMode(CENTER);
  image(startScreen, width/2,height/2);
  
  if(mouseX > width/4 - 100 && mouseX < width/4 + 100 && mouseY > height*0.7f - 50 && mouseY < height*0.7f + 50){
    // Expand icon when moused over
    image(start, width/4, height*0.7f, 220, 110);
    if(mouseReleased){
      currentLevel = 1;
    }
  }
  else{
    image(start, width/4, height*0.7f, 200, 100);
  }
  
  if(mouseX > 3*width/4 - 100 && mouseX < 3*width/4 + 100 && mouseY > height*0.7f - 50 && mouseY < height*0.7f + 50){
    // Expand icon when moused over
    image(levels, 3*width/4, height*0.7f, 220, 110);
    if(mouseReleased){
      currentLevel = -1;
    }
  }
  else{
    image(levels, 3*width/4, height*0.7f, 200, 100);
  }
}
// SHOOTER TYPES:
// 0 - regular, shoots at set direction
// 1 - sniper, shoots in player's direction
// 2 - berserker, rotates around rapidly, shooting in all directions

public void level1(){
  // Easy level, introduce basic concepts
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height/2));
  myPortal = new Portal(new PVector(width-shipDiam, height/2));
  
  shooters.add(new Shooter(0, new PVector(width/4, 0), 60, new PVector(0, 1)));
  shooters.add(new Shooter(0, new PVector(width/2, height), 60, new PVector(0, -1)));
  shooters.add(new Shooter(0, new PVector(width*0.75f, 0), 60, new PVector(0, 1)));
  
  globalResets();
}

public void level2(){     
  // Easy as well, just showing you bullets can be diagonal
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height/2));
  myPortal = new Portal(new PVector(width-shipDiam, height/2));
 
  shooters.add(new Shooter(0, new PVector(300, 0), 60, new PVector(1, 1)));
  shooters.add(new Shooter(0, new PVector(300, height), 60, new PVector(1, -1)));
  
  globalResets();
}

public void level3(){     
  // What if we combined them?
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height*0.9f));
  myPortal = new Portal(new PVector(width-shipDiam, height*0.05f));
 
  shooters.add(new Shooter(0, new PVector(0, 250), 60, new PVector(1, 0)));
  shooters.add(new Shooter(0, new PVector(0, 400), 60, new PVector(1, 0)));
  shooters.add(new Shooter(0, new PVector(0, 550), 60, new PVector(1, 0)));
  shooters.add(new Shooter(0, new PVector(250, height), 60, new PVector(0, -1)));
  shooters.add(new Shooter(0, new PVector(400, height), 60, new PVector(0, -1)));
  shooters.add(new Shooter(0, new PVector(550, height), 60, new PVector(0, -1)));
  shooters.add(new Shooter(0, new PVector(700, 0), 60, new PVector(1, 1)));
  shooters.add(new Shooter(0, new PVector(850, 0), 60, new PVector(1, 1)));
  shooters.add(new Shooter(0, new PVector(1000, 0), 60, new PVector(1, 1)));
  
  globalResets();
}

public void level4(){
  // What if we combined them and made it even harder?
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height/2));
  myPortal = new Portal(new PVector(width-shipDiam, height/2));
  
  shooters.add(new Shooter(0, new PVector(0, 0), 60, new PVector(1, 0.6f)));
  shooters.add(new Shooter(0, new PVector(0, height), 60, new PVector(1, -0.6f)));
  shooters.add(new Shooter(0, new PVector(width, 0), 60, new PVector(-1, 1)));
  shooters.add(new Shooter(0, new PVector(width, height), 60, new PVector(-1, -1)));
  shooters.add(new Shooter(0, new PVector(width/3, 0), 60, new PVector(0, 1)));
  shooters.add(new Shooter(0, new PVector(2*width/3, height), 60, new PVector(0, -1)));
  
  globalResets();
}

public void level5(){
  // Teach the player "HEY MY BULLETS CAN FOLLOW YOU"
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height*0.1f));
  myPortal = new Portal(new PVector(width-shipDiam, height*0.9f));
  
  shooters.add(new Shooter(0, new PVector(width/3, 0), 60, new PVector(0, 1)));
  shooters.add(new Shooter(1, new PVector(2*width/3, height), 60));
  
  globalResets();
}

public void level6(){
  // Teach the player "I'M STILL FOLLOWING YOU"
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height*0.1f));
  myPortal = new Portal(new PVector(width-shipDiam, height*0.9f));
  
  shooters.add(new Shooter(0, new PVector(width/2, 0), 60, new PVector(0, 1)));
  shooters.add(new Shooter(1, new PVector(width/2, height), 60));
  shooters.add(new Shooter(1, new PVector(3*width/4, 0), 60));
  shooters.add(new Shooter(0, new PVector(3*width/4, height), 60, new PVector(0, -1)));  
  
  globalResets();
}

public void level7(){
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height/2));
  myPortal = new Portal(new PVector(width-shipDiam, height/2));
  
  shooters.add(new Shooter(1, new PVector(width/2, 0), 60));
  shooters.add(new Shooter(1, new PVector(width/2, height), 60));
  shooters.add(new Shooter(1, new PVector(3*width/4, height), 60));
  shooters.add(new Shooter(1, new PVector(3*width/4, 0), 60));
  shooters.add(new Shooter(0, new PVector(width, 0), 60, new PVector(-1, 1)));
  shooters.add(new Shooter(0, new PVector(width, height), 60, new PVector(-1, -1)));
  
  globalResets();
}

public void level8(){
  // Teach the player true pain
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height*0.1f));
  myPortal = new Portal(new PVector(width-shipDiam, height*0.9f));
  
  shooters.add(new Shooter(2, new PVector(width/2, height/2), 6));
  
  globalResets();
}

public void level9(){
  // This is getting really difficult
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height*0.1f));
  myPortal = new Portal(new PVector(width-shipDiam, height*0.9f));
  
  shooters.add(new Shooter(2, new PVector(width/3, height/2), 12));
  shooters.add(new Shooter(2, new PVector(2*width/3, height/2), 12));
  
  globalResets();
}

public void level10(){
  // TRY THIS
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height/2));
  myPortal = new Portal(new PVector(width-shipDiam, height/2));
  
  shooters.add(new Shooter(1, new PVector(width/3, 0), 60));
  shooters.add(new Shooter(1, new PVector(width/3, height), 60));
  shooters.add(new Shooter(2, new PVector(5*width/7, height/2), 6));
  
  globalResets();
}

public void level11(){
  // and this
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height/2));
  myPortal = new Portal(new PVector(width-shipDiam, height/2));
  
  shooters.add(new Shooter(1, new PVector(4*width/7, height/2), 60));;
  shooters.add(new Shooter(2, new PVector(5*width/7, 3*height/8), 9));
  shooters.add(new Shooter(2, new PVector(5*width/7, 5*height/8), 9));
  
  globalResets();
}

public void level12(){
  // Pain: The Level
  bullets = new ArrayList<Bullet>();
  shooters = new ArrayList<Shooter>();
  
  myShip = new Ship(shipDiam, new PVector(shipDiam, height/2));
  myPortal = new Portal(new PVector(width-shipDiam, height/2));
  
  shooters.add(new Shooter(2, new PVector(4*width/7, 0), 9));
  shooters.add(new Shooter(2, new PVector(4*width/7, height), 9));
  shooters.add(new Shooter(2, new PVector(3*width/7, 0), 9));
  shooters.add(new Shooter(2, new PVector(3*width/7, height), 9));
  shooters.add(new Shooter(1, new PVector(0, height), 60));
  
  globalResets();
}
  public void settings() {  size(1200, 700); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--present", "--window-color=#666666", "--stop-color=#cccccc", "bullet_time" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
