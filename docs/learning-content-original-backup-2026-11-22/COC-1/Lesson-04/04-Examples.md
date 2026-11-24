# COC 1 — Lesson 4: Creation of Portable Bootable Devices

## Examples

Examples:

Bootable USB flash drive

Bootable DVD

Recovery drive

Linux live USB

C. Requirements

USB flash drive with sufficient capacity (commonly 8 GB or more for Windows installation media, depending on the image).

A valid operating system ISO image.

Bootable USB creation software.

A working computer.

Internet connection when downloading the ISO.

D. Procedure for Creating a Bootable USB Using Rufus

Download Rufus from its official website: https://rufus.ie/

Download a legitimate ISO image from the operating system's official source.

Insert the USB flash drive.

Open Rufus.

Select the correct USB device.

Click the SELECT button and choose the ISO image.

Choose the partition scheme appropriate to the target system:

GPT for most modern UEFI systems.

MBR for compatible legacy BIOS systems.

Verify the selected device and settings.

Click START.

Read the warning that the USB drive will be erased.

Confirm only if the correct device is selected and important files are backed up.

Wait until the process is completed.

Safely eject the USB drive.

Test the USB using the computer's boot menu.

VIDEO - https://youtu.be/uHPsiZduZ5w?si=dMANDJiyHsmQeZGS

E. Important Reminders

Creating bootable media erases the selected USB drive.

Always back up USB files first.

Use a trusted ISO image.

Do not choose the wrong storage device.

Boot settings depend on the target computer's firmware.

4.2 How to Create an ISO Image Using Third-Party Applications or DOS Commands

A. Third-Party Applications

Examples:

ImgBurn

Used to create ISO images from compatible discs or files.

Can write image files to optical media.

PowerISO

Creates, edits, and extracts ISO images.

Supports mounting image files.

AnyBurn

Used to create and burn disc image files.

Rufus

Primarily used to create bootable USB installation media from ISO images.

B. General Procedure for Creating an ISO Image

Install a trusted ISO creation application.

Open the program.

Choose the option to create an image file.

Select the source disc or files.

Choose the destination folder.

Enter the ISO filename.

Start the creation process.

Wait for completion.

Verify the image file.

Test or mount the ISO as needed.

VIDEO - https://youtu.be/XTG7FGULpVk?si=Qi6YLtgsdlJQoKnw

C. Creating an ISO Image Through Windows Commands

Windows does not include a universal built-in command that creates an ISO from any folder. A third-party command-line utility can be used.

Example using oscdimg from the Windows Assessment and Deployment Kit (ADK):

oscdimg -m -o -u2 -udfver102 C:\ISO_Source C:\Output\MyImage.iso

Explanation:

oscdimg – command-line utility for creating ISO images.

-m – allows a larger image size.

-o – optimizes storage by using duplicate files efficiently.

-u2 – creates a UDF file system.

-udfver102 – specifies UDF version 1.02.

C:\ISO_Source – source folder.

C:\Output\MyImage.iso – destination ISO file.

VIDEO - https://youtu.be/sDr59oM1row?si=vHw2tJEQPhysQD2v

D. Basic DOS/Command Prompt Disk Preparation Commands

The following commands are for disk preparation and management, not universal ISO creation.
