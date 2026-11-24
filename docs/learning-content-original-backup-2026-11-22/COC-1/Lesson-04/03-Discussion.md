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

4.2 How to Create an ISO Image Using Third-Party Applications or DOS Commands
A. Third-Party Applications
Examples:
1.	ImgBurn
1.	Used to create ISO images from compatible discs or files.
2.	Can write image files to optical media.
2.	PowerISO
1.	Creates, edits, and extracts ISO images.
2.	Supports mounting image files.
3.	AnyBurn
1.	Used to create and burn disc image files.
4.	Rufus
1.	Primarily used to create bootable USB installation media from ISO images.
B. General Procedure for Creating an ISO Image
1.	Install a trusted ISO creation application.
2.	Open the program.
3.	Choose the option to create an image file.
4.	Select the source disc or files.
5.	Choose the destination folder.
6.	Enter the ISO filename.
7.	Start the creation process.
8.	Wait for completion.
9.	Verify the image file.
10.	Test or mount the ISO as needed.

C. Creating an ISO Image Through Windows Commands
Windows does not include a universal built-in command that creates an ISO from any folder. A third-party command-line utility can be used.
Example using oscdimg from the Windows Assessment and Deployment Kit (ADK):
oscdimg -m -o -u2 -udfver102 C:\ISO_Source C:\Output\MyImage.iso
Explanation:
11.	oscdimg – command-line utility for creating ISO images.
12.	-m – allows a larger image size.
13.	-o – optimizes storage by using duplicate files efficiently.
14.	-u2 – creates a UDF file system.
15.	-udfver102 – specifies UDF version 1.02.
16.	C:\ISO_Source – source folder.
17.	C:\Output\MyImage.iso – destination ISO file.

D. Basic DOS/Command Prompt Disk Preparation Commands
The following commands are for disk preparation and management, not universal ISO creation.
Command	Function
diskpart	Opens the disk management command utility
list disk	Displays connected disks
list volume	Displays volumes
select disk	Selects a disk
format	Formats a volume
assign	Assigns a drive letter

