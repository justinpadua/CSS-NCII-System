# COC 1 — Lesson 4: Creation of Portable Bootable Devices

## Examples

### Prepare authorized operating-system installation media

A technician needs a bootable USB for an authorized lab installation. First, the technician obtains installation media from the operating-system publisher and checks the target computer's firmware requirements. The technician backs up the USB contents, verifies the drive's identity and capacity, then uses the publisher's media-creation tool or a trusted imaging utility to write the existing image to that USB. The selected USB is erased, so the technician checks the target again before confirming.

An ISO file is an image file; having an ISO does not by itself mean a USB has been prepared. DiskPart manages disks, partitions, and volumes. It does not create ISO images. A command that builds an ISO from a folder also needs the appropriate boot files and boot-image configuration if the result is intended to boot.
