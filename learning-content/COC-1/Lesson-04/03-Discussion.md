COC 1 — LESSON 4: Discussion
4.1 How to Create a Functional ISO Image and Bootable Device
A. What is an ISO Image?
An ISO image is a file containing the data and structure of a disc or installation media.
Uses:
1.	Operating system installation.
2.	Software distribution.
3.	System recovery.
4.	Backup of installation media.
5.	Creating bootable USB drives.

B. What is a Bootable Device?
A bootable device is a storage device that contains the files and boot structures required to start a computer.
Examples:
1.	Bootable USB flash drive
2.	Bootable DVD
3.	Recovery drive
4.	Linux live USB
C. Requirements
1.	USB flash drive with sufficient capacity (commonly 8 GB or more for Windows installation media, depending on the image).
2.	A valid operating system ISO image.
3.	Bootable USB creation software.
4.	A working computer.
5.	Internet connection when downloading the ISO.
D. Procedure for Creating a Bootable USB Using Rufus
1.	Download Rufus from its official website: https://rufus.ie/
2.	Download a legitimate ISO image from the operating system's official source.
3.	Insert the USB flash drive.
4.	Open Rufus.
5.	Select the correct USB device.
6.	Click the SELECT button and choose the ISO image.
7.	Choose the partition scheme appropriate to the target system:
1.	GPT for most modern UEFI systems.
2.	MBR for compatible legacy BIOS systems.
8.	Verify the selected device and settings.
9.	Click START.
10.	Read the warning that the USB drive will be erased.
11.	Confirm only if the correct device is selected and important files are backed up.
12.	Wait until the process is completed.
13.	Safely eject the USB drive.
14.	Test the USB using the computer's boot menu.
VIDEO - https://youtu.be/uHPsiZduZ5w?si=dMANDJiyHsmQeZGS

E. Important Reminders
1.	Creating bootable media erases the selected USB drive.
2.	Always back up USB files first.
3.	Use a trusted ISO image.
4.	Do not choose the wrong storage device.
5.	Boot settings depend on the target computer's firmware.

4.2 ISO files, bootable media, and disk commands

An ISO is a disc-image file. Creating a data ISO from a folder is different from preparing bootable installation media. A bootable image must contain the correct operating-system files and boot configuration for its intended firmware and architecture. A command that merely packages a folder into an ISO does not necessarily make it bootable.

For a supported operating-system installer, obtain the image or media from the operating-system publisher and use its documented media-creation process or a trusted tool such as Rufus. Selecting a USB device for writing installation media normally erases it. Back up its contents, verify the device identity, and follow the target computer's UEFI/firmware requirements. GPT is commonly used with UEFI installations and MBR with legacy BIOS, but follow OS and hardware instructions for the actual target.

`oscdimg` is a Windows ADK command-line tool for creating ISO images from prepared files. Its boot-related options require the correct boot files and configuration; do not treat a generic data-ISO command as a complete bootable-media recipe. DiskPart manages disks, partitions, and volumes; it does not create ISO files. Disk commands can destroy data, so use them only with authorization and after verifying the selected disk.

Before installation, obtain authorization, back up important data, verify the target drive, and explain the data-loss risk. Use legitimate installation media and trusted software sources.

