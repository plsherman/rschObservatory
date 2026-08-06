# rschObservatory
Research observatory code base rPi3
2026/07/05  git push java source code that works on new pi using a 
			breakout test board. All sensor & relay pins verified
			as functioning according to program documentation. 
			Code base used was from backup foles located on Phil's
			laptop. Updates are written and pushed to github from both
			systems. Multiple push commands to lock in no pw push.
			
			
			ALWAYS DO git fetch THEN git status TO VERIFY SOURCE ON
			CURRENT SYSTEM IS CURRENT BEFORE STARTING CODE CHANGES.


 ***********  WARNING  WARNING  WARNING  ********************

The link to obs-server.service is a hard link to allow git to store the file
If this file is modified, the link must be rebuilt since it's assumed that
    the inode will change. Run the following 3 commands to fix this

 cd /home/pi/gitRepos/rschObservatory
 rm etc*
 ln /etc/systemd/system/obs-server.service etc.systemd.system.obs-server.service

 *********** END WARNING    END WARNING    END WARNING  ********************