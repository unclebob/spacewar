# Space War

A Star Trek Game.

>Play the current release at http://cleancoder.com/space-war 
> (The server is slow so be patient while it loads, the cache is set for 24 hours)

>_Courtesy of Mike Fikes (@mfikes) you can play a very old version at http://spacewar.fikesfarm.com/spacewar.html_

The Borg have been defeated; but the federation fleet has been destroyed.
The enterprise is the only star ship left in service.  A few bases remain, but the 
Borg completely disrupted the antimatter and dilithium transport network.

The Klingons in Federation space took a beating as well.  But the Borg didn't like the taste of
Klingons in the "collective", so they simply left many crippled Klingon ships
stranded in Federation space rather than assimilating them.  

There are rumors that the Klingon fleet escaped total destruction and that the 
remaining ships are regrouping in Klingon space in order to form an invasion fleet. 
If so, it won't be long until they arrive in force.

![screenshot](https://github.com/unclebob/spacewar/spacewar.jpg)

## Your Mission

> Build the _Corbomite_ device and use it to thwart the Klingon invasion!  

All Federation shipyards were obliterated by the Borg.  No new ships can be ready in time.

You will have to reconstitute a network of bases
that you can use to supply yourself with antimatter, dilithium, and weapons.  Harvest
what you can from the crippled Klingons.  Build _The Corbomite Device_ before the
invasion fleet arrives in force. 

Once you have enough production going, find _The Pulsar_ and build the _Corbomite_ factory there.
Surround that factory with plenty of Antimatter and Dilithium bases so that it can manufacture
enough _Corbomite_.  

Once the _Corbomite_ factory has been filled to capacity, it will transform into 
_The Corbomite Device_.  When you dock with _The Corbomite Device_ your ship will become
much more powerful, and you should be able to rid the federation of all klingons.

## Controls
Most of the controls are simple buttons or sliders.  You should not find them difficult to use.
However, three controls (_ENGAGE_, _FIRE_, and _New Game_) require a long press to activate.
This is to prevent accidental activation.  You must hold the button down until it turns white.

## Sensors
You have three views.  

 * Front view, which shows you a pretty picture
of stars, but is otherwise useless. 

 * STRAT. is the strategic view of the whole galaxy.  You can zoom in and out using the
 slider.  You cannot see any weapons, explosions, or debris in this view; but you _can_ see Klingons, Romulans, stars,
 bases, transports, and transport routes.
 
 * TACT.  This is the tactical view.  You can see what is nearby, including all weapons that you
 or your enemies have fired.  You'll also see explosions.
 
## Engines
You have two modes of movement.  Warp, and Impulse.

 * The WARP engines move you in a sequence of leaps.  Each leap
 is the same size.  The more power you use the faster those leaps
 are made.  Be careful, the energy consumed is not linear.  Warp 10
 uses a _lot_ more antimatter than Warp 1.  And, remember, rapid energy
 consumption increases the Core Temperature; especially if you are low on Dilithium.
 
 * The Impulse engines are reaction thrust engines.  They consume antimatter and
 can propel you at speeds that are a significant fraction of C.  At those speeds
 space itself has a viscosity.  The ship feels a drag that limits the velocity, and will
 rapidly slow the ship when the engines are turned off.  
 
 * The Heading control turns the ship.  Both the Impulse and Warp engines
 move the ship in the direction it is facing.
 
## Weapons
You have three weapons.  Phasers, Torpedos, and Kinetics.  
 * Phasers are powerful rays that move quickly and do a lot of damage.
  However, they are short range and lose effectiveness rapidly with distance.
  If you want to use them, get close.  They are great in a stern chase, or a game of chicken.
    
 * Torpedos are slow, but pack a significant punch.  
  
 * Kinetics are just cannon balls moving at relativistic velocity.  They do a small amount of damage but are cheap to shoot.
  
You can fire all of these weapons in a spread if you like.
Choose the number of shots and the spread angle with the sliders.
Aim by clicking on the screen where you want the center of your spread to hit, and
remember to lead your target.

Be careful, you can use your torpedoes up fast; and shooting phasers
really heats up the core.
  
All weapons consume antimatter.  

Weapons are not very accurate when fired from within a warp field.
  
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
 
 As long as either LIF or HUL are not black, these systems will be gradually repaired.
 So it is often a good idea to warp away from a bad situation and wait
 the repairs out.
 
## Docking
 To replenish your antimatter, and weapons, you'll need to dock at a base.
 You do this by warping close and then using your impulse engines to approach
 the base as closely as you can.  Put the ship on top of the base.  Watch for the 
 _DOCK_ button to show up in the Engine Control Panel.  Hit that button to be refueled and restocked.
 
## Bases 
 There are four kinds of bases.  Antimatter factories, Dilithium factories, Weapon factories, 
 and _The Corbomite Factory_.
  * Antimatter factories manufacture antimatter using the power from nearby O, B, or A
  class stars (the big blue or white ones).
  * Dilithium factories harvest dilithium from the atmospheres of nearby K or M class stars.
  (the little red or orange ones.)
  * Weapon factories manufacture Kinetics and Torpedos.  They require the elements found 
  around the planets of F or G class stars (The medium sized yellow stars like our Sun).  
  * _Corbomite_ factories manufacture _Corbomite_.  Only one can be built, and it must be built
  adjacent to _The Pulsar_.  When supplied with a ready stream of antimatter and dilithium
  it will manufacture _Corbomite_.
  
  You can deploy a base by gradually approaching the appropriate kind of star.  When you
  are in deployment range, the appropriate deploy button will appear in the Deploy panel.   
  
  Deploying a base uses up a _lot_ of antimatter and dilithium; so be careful.
  
  Bases do not start making products right away, there is a startup time.  During that time you
  will see the base partially obscured.  Afterwards the bases start slowly manufacturing products. 
  
## Transport routes
  On the strategic display you will find green lines that connect some of the bases.
  Cargo transport vessels follow these routes to supply the bases with the resources they need.
  Yellow transports carry dilithium.  Orange transports carry antimatter.  They travel at just over Warp 1.
  
  Factories work to build up a reserve.  Once that reserve is exceeded, factories will transport
  goods to connected factories that need those goods.  Weapons factories and _The Corbomite Factory_
  are resource sinks.  Dilithium factories are antimatter sinks.  Be careful how you lay
  them out.  
  
  Transport routes have a maximum length.  Bases that are too far apart will not ship goods to each other.  
  
## Production
  Weapon bases use antimatter to create kinetics.  This production is quite rapid.  Torpedoes require
  substantial amounts of both antimatter and dilithium to create.  Make sure you have the weapon factories 
  connected, either directly, or indirectly, to antimatter and dilithium factories.  Take care, a weapon factory
  can consume large amounts of dilithium. 
  
  Dilithium factories require small amounts of antimatter to produce dilithium.  Dilithium
  production is slow, so you will likely need many of these factories. 
 
## Klingons
  Some of the crippled Klingons are still pretty strong.  Be careful.  Others
  are so weakened that they'll run from you and won't put up much of a fight.  They can detect
  you at long range.  So expect visits from the stronger Klingons.
  
  Klingons are slow because they never learned dilithium catalysis. Any dilithium
  they have on board was probably stolen and is is hoarded to deplete Federation reserves.
  
  Klingons can collect antimatter from all stars, though they collect faster from O and A stars,
  and only very slowly from K and M stars.  You will often see them
  hovering around a star to refuel.  Klingons are also more than willing to
  steal antimatter from a base.
  
  Klingons have the facilities on board to construct torpedos and kinetics.  This takes lots
  of antimatter.  A Klingon hovering by a base or a star may be building up their
  arsenal.

Desperate klingons will sometimes go kamikazee on you.  They'll overload their engines
to get close enough to kill you before their engines explode.
  
## Blockades
  Klingons will sometimes invade your bases and take them over. 
  Blockaded bases will not ship any materials to other bases.  Materials manufactured by, or
  shipped to a blockaded base will be acquired by the occupying Klingons.
  
  Beware, more than one Klingon can occupy a base.  If you challenge a base with three or four
  Klingons occupying it; be prepared for a pitched battle.  They often fly in formation,
  so it can be hard to know how many are flying together.
  
## Klingon Battle Strategy
  Once your ship is in range of a Klingon's short range detectors, the Klingon will head towards you to do battle.  
  The Klingon strategy is to confuse and delude you.  They accomplish this by continuously
  changing tactics.  Stay alert to their movements in the heat of battle; and don't let them
  get too close.  At short range their disrupters are devastating. 
  
  When a Klingon is critically low on resources, there is a good chance it will run away.  This
  is a good time to attack them.  Be careful though, the retreat may be a ruse.
  
## Dilithium Clouds
  When you destroy a Klingon vessel, their stolen on-board dilithium will disperse amidst
  the cloud of debris.  You can harvest this dilithium by easing yourself as close
  as possible into the _center_ of the cloud.  Dilithium, when exposed to the vacuum,
  has a half-life of about a minute.  So hurry.
  
## Romulans
  Romulans are marauders.  Their ships are cloaked and quite difficult to detect.
  They fire an omni-directional energy weapon that does
  tremendous damage and moves at roughly Warp 5.  Fortunately they cannot remain
  cloaked while energizing or firing their weapon.  During that very brief period
  they are quite vulnerable.  A single shot will kill them.  
  
  A hit at short range from their weapon will completely drain your shields, do significant damage
  to your systems, and has a one in three chance of killing you outright.  When they appear, you have two options:  
  Fight, or run. If you choose to fight,
  make sure you hit them before they fire their weapon.  If you choose to run,
  run fast and far; the weapon loses potency with distance. 
  
## _The Pulsar_
  There is only one _Pulsar_ in federation space.  It is small and hard to find.  It has a greenish
  tinge and flashes dimly.  This is the only star that can support a _Corbomite_ factory.

## _The Corbomite Device_
Once you have docked with _The Corbomite Device_ your warp speed will be enhanced.  Weapons and sheilds will become
much more effective.  You should be able to kill all the remaining Klingons and withstand
their defenses.  You'll notice that they will all start retreating and heading back to their
home world of Praxis.  Some will likely run out of fuel in their panic to escape.  You can
pick them off with one or two kinetics.

But be careful.  _The Corbomite Device_ does not make you invulnerable.  If you are careless
enough to get yourself killed, you will not be reincarnated with the corbomite device; and the 
Klingons will resume their agressive behavior.
  
## Saving and Pausing
  The game is saved every five seconds.  You pause the game by quitting.  Your previous
  game will resume when you start the program up again.  If you die, you will be resurrected
  in a random location with partial resources.  If you want to start a new game, use the _New Game_
  button at the top left or delete the `spacewar.world` file before starting the game.
  
  On the desktop, the game is saved in a file named: `spacewar.world`.  You can rename this file if you want to
  set the current game aside and start a new one.  You bring a set-aside game back by
  renaming it to `spacewar.world`.  
   
 
