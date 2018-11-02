# spacewar

A Star Trek Game.

The Borg have defeated; but the federation fleet has been destroyed.
The enterprise is the only star ship left in service.  

The Klingons are taking this opportunity to invade.  You must stop
the invasion.

### Sensors
You have three views.  

 * Front view, which shows you a pretty picture
of stars, but is otherwise useless. 

 * STRAT. is the strategic view of the whole galaxy.  You can zoom in and out using the
 slider.  You cannot see any weapons in this view; but you can see Klingons, stars, and bases.
 
 * TACT.  This is the tactical view.  You can see what is nearby, including all weapons that you
 or your enemies have fired.  You'll also see explosions.
 
### Engines
You have two modes of movement.  Warp, and Impulse.

 * The WARP engines move you in a sequence of leaps.  Each leap
 is the same size.  The more power you use the faster those leaps
 are made.  Be careful, the energy consumed is not linear.  Warp 10
 uses a _lot_ more antimatter than Warp 1.  And, remember, rapid energy
 consumption increases the Core Temperature; especially if are low on Dilithium.
 
 * The Impulse engines are reaction thrust engines.  They consume antimatter and
 can propel you at speeds that are a significant fraction of C.  At those speeds
 space itself has a viscosity.  The ship feels a drag that limits the velocity, and will
 rapidly slow the ship when the engines are turned off.  
 
 * The Heading control turns the ship.  Both the Impulse and Warp engines
 move the ship in the direction it is facing.
 
 ### Weapons
You have three weapons.  Phasers, Torpedos, and Kinetics.  
 * Phasers are powerful rays that move quickly and do a lot of damage.
  However, they are short range and lose effectiveness rapidly with distance.
  If you want to use them, get close.
  
 * Torpedos are slow, but pack a significant punch.  
  
 * Kinetics are just cannon balls moving at relativistic velocity.
  They do a small amount of damage but are cheap to shoot.
  
You can fire all of these weapons in a spread if you like.
Choose the number of shots and the spread angle with the sliders.
Be careful, you can use your torpedoes up fast; and shooting phasers
really heats up the core.
  
All weapons consume antimatter.  
  
## Status
There are four things you need to watch at all times.
  
 * Antimatter is your fuel.  If you run out, you'll be stranded.  Don't run out.

 * Dilithium is the catalyst for the antimatter reaction.  It gets gradually
   consumed by all operations.  When you start running low you'll notice your
   core temperature increasing.  This is because the antimatter reaction becomes less efficient.

 * Core Temperature needs to stay out of the red.  Get too hot and you blow.

 * Shields.  Shields absorb the energy of hits against you.
   They automatically replenish by consuming antimatter.  You will start
   to experience real damage if the shields get below 50%.  
   
## Damage
There are six damage indicators.  LIF, HUL, SEN, WPN, WRP, IMP.
These indicators have four states.  Green, Yellow, Red, and Black.
As you might imagine, black is bad.  It means the system is entirely 
non-functional.
    
 * LIF - Life support systems.  The more damaged this system is, the 
 slower repair operations are performed.  If it goes black, you die.
 Don't let it go black.
 
 * HUL - This is the hull.  It is the first thing repaired after LIF.
 If the HUL goes black, you die.  Don't let it go black.
 
 * SEN - These are the sensors.  As they degrade, you'll know it.
 If it goes black, you'll be blind.
 
 * WPN - These are your weapons.  As they degrade the weapons will
 misfire and misbehave.  Be careful!
 
 * IMP - Your impulse engines.  As they degrade, you'll note a
 pronounced tendency for the ship to stall.
 
 * WRP - Your warp engines.  As they degrade you'll notice the
 warp leaps become erratic.  
 
 As long as LIF is not black, these systems will be gradually repaired.
 So it is often a good idea to warp away from a bad situation and wait
 the repairs out.
 
 ## Docking
 To replenish your antimatter, and weapons, you'll need to dock at a base.
 You do this by warping close and then using your impulse engines to approach
 the base as closely as you can.  Put the ship on top of the base.  Watch for the 
 _DOCK_ button to show up in the Engine Control Panel.  Hit that button
 and you'll be refueled and restocked.
 
