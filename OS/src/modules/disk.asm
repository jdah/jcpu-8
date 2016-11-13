; Disk and filesystem operations

; JIFS FILESYSTEM SPECIFICATION:
; Disk is divided up into 256 byte 'blocks'. Each block contains either part of a file or a directory.
; Filenames are all capital letters, in format 13.3 (13 for name, 3 for extension).
; The first block in the filesystem gives a little info on the filesystem and contains a bitmap of used
; and free blocks in the filesystem.
; Filesystem supports a maximum of a 16 MiB disk, limited by the 16-bit block index. ((2 ^ 16) * 256) = 16 MiB.

; First block specification:
; 2 bytes - The number of blocks in the filesystem, including those in the superblock
; 254 bytes - Empty (Use for other info later on...)
; Blocks 1-32 serve as a bitmap of used and free blocks for a maximum of 65536 blocks.

; The root (and only) directory is stored on blocks 33-64.
; The only purpose of the root directory is to serve as a list of all files on the disk. The list structure
; maxes out at about 455 entries.
; List structured as follows:
; 16 bytes - file name
; 2 bytes - block index for the start of the file

; The first block of a file is structured like this:
; 2 bytes - file size (in blocks)
; 2 bytes - index of next block (0x0000 if there isn't one)
; 2 bytes - number of bytes used in this block (250 if all are being used)

; And any other blocks are structured like this:
; 2 bytes - index of next block (0x0000 if there isn't one)
; 2 bytes - number of bytes used in this block (252 if all are being used)

; Read a 256 byte block from the disk
; PARAMS: AB = block index
; RETURN: CurrentBlock = data
#disk_read_block:
	push a
	push b
	push c
	push d
	push e

	; High storage address is index / 512
	push a
	push b
	lc c, 0x02
	lc d, 0x00
	call int_16_div
	call rpi_storage_addr_high
	pop b
	pop a

	; Low high is (index % 512) / 2. Stored in E.
	lc c, 0x02
	lc d, 0x00
	call int_16_mod

	push a
	push b
	lc c, 0x00
	lc d, 0x02
	call int_16_div
	mw b, e
	pop a
	pop b

; The number of blocks in the current filesystem, stored as a 16-bit number
#db NumBlocksH 0x00
#db NumBlocksL 0x00

; Reserve 256 bytes of space for block processing
#resb CurrentBlock 256