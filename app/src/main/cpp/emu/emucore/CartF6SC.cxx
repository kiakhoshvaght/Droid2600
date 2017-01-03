//============================================================================
//
//   SSSS    tt          lll  lll       
//  SS  SS   tt           ll   ll        
//  SS     tttttt  eeee   ll   ll   aaaa 
//   SSSS    tt   ee  ee  ll   ll      aa
//      SS   tt   eeeeee  ll   ll   aaaaa  --  "An Atari 2600 VCS Emulator"
//  SS  SS   tt   ee      ll   ll  aa  aa
//   SSSS     ttt  eeeee llll llll  aaaaa
//
// Copyright (c) 1995-2016 by Bradford W. Mott, Stephen Anthony
// and the Stella Team
//
// See the file "License.txt" for information on usage and redistribution of
// this file, and for a DISCLAIMER OF ALL WARRANTIES.
//
// $Id: CartF6SC.cxx 3316 2016-08-24 23:57:07Z stephena $
//============================================================================

#include "System.hxx"
#include "CartF6SC.hxx"

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
CartridgeF6SC::CartridgeF6SC(const uInt8* image, uInt32 size, const Settings& settings)
    : Cartridge(settings),
    myCurrentBank(0)
{
  // Copy the ROM image into my buffer
    memcpy(myImage, image, std::min(16384u, size));
    createCodeAccessBase(16384);

    // Remember startup bank
    myStartBank = 0;
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
void CartridgeF6SC::reset()
{
    initializeRAM(myRAM, 128);

    // Upon reset we switch to the startup bank
    bank(myStartBank);
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
void CartridgeF6SC::install(System& system)
{
    mySystem = &system;

    System::PageAccess access(this, System::PA_READ);

    // Set the page accessing method for the RAM writing pages
    access.type = System::PA_WRITE;
    for (uInt32 j = 0x1000; j < 0x1080; j += (1 << System::S_PAGE_SHIFT))
    {
        access.directPokeBase = &myRAM[j & 0x007F];
        access.codeAccessBase = &myCodeAccessBase[j & 0x007F];
        mySystem->setPageAccess(j >> System::S_PAGE_SHIFT, access);
    }

    // Set the page accessing method for the RAM reading pages
    access.directPokeBase = 0;
    access.type = System::PA_READ;
    for (uInt32 k = 0x1080; k < 0x1100; k += (1 << System::S_PAGE_SHIFT))
    {
        access.directPeekBase = &myRAM[k & 0x007F];
        access.codeAccessBase = &myCodeAccessBase[0x80 + (k & 0x007F)];
        mySystem->setPageAccess(k >> System::S_PAGE_SHIFT, access);
    }

    // Install pages for the startup bank
    bank(myStartBank);
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
uInt8 CartridgeF6SC::peek(uInt16 address)
{
    uInt16 peekAddress = address;
    address &= 0x0FFF;

    // Switch banks if necessary
    switch (address)
    {
        case 0x0FF6:
          // Set the current bank to the first 4k bank
            bank(0);
            break;

        case 0x0FF7:
          // Set the current bank to the second 4k bank
            bank(1);
            break;

        case 0x0FF8:
          // Set the current bank to the third 4k bank
            bank(2);
            break;

        case 0x0FF9:
          // Set the current bank to the forth 4k bank
            bank(3);
            break;

        default:
            break;
    }

    if (address < 0x0080)  // Write port is at 0xF000 - 0xF080 (128 bytes)
    {
      // Reading from the write port triggers an unwanted write
        uInt8 value = mySystem->getDataBusState(0xFF);

        if (bankLocked())
            return value;
        else
        {
            triggerReadFromWritePort(peekAddress);
            return myRAM[address] = value;
        }
    }
    else
        return myImage[(myCurrentBank << 12) + address];
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
bool CartridgeF6SC::poke(uInt16 address, uInt8)
{
    address &= 0x0FFF;

    // Switch banks if necessary
    switch (address)
    {
        case 0x0FF6:
          // Set the current bank to the first 4k bank
            bank(0);
            break;

        case 0x0FF7:
          // Set the current bank to the second 4k bank
            bank(1);
            break;

        case 0x0FF8:
          // Set the current bank to the third 4k bank
            bank(2);
            break;

        case 0x0FF9:
          // Set the current bank to the forth 4k bank
            bank(3);
            break;

        default:
            break;
    }

    // NOTE: This does not handle accessing RAM, however, this function
    // should never be called for RAM because of the way page accessing
    // has been setup
    return false;
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
bool CartridgeF6SC::bank(uInt16 bank)
{
    if (bankLocked()) return false;

    // Remember what bank we're in
    myCurrentBank = bank;
    uInt16 offset = myCurrentBank << 12;

    System::PageAccess access(this, System::PA_READ);

    // Set the page accessing methods for the hot spots
    for (uInt32 i = (0x1FF6 & ~System::S_PAGE_MASK); i < 0x2000;
        i += (1 << System::S_PAGE_SHIFT))
    {
        access.codeAccessBase = &myCodeAccessBase[offset + (i & 0x0FFF)];
        mySystem->setPageAccess(i >> System::S_PAGE_SHIFT, access);
    }

    // Setup the page access methods for the current bank
    for (uInt32 address = 0x1100; address < (0x1FF6U & ~System::S_PAGE_MASK);
        address += (1 << System::S_PAGE_SHIFT))
    {
        access.directPeekBase = &myImage[offset + (address & 0x0FFF)];
        access.codeAccessBase = &myCodeAccessBase[offset + (address & 0x0FFF)];
        mySystem->setPageAccess(address >> System::S_PAGE_SHIFT, access);
    }
    return myBankChanged = true;
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
uInt16 CartridgeF6SC::getBank() const
{
    return myCurrentBank;
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
uInt16 CartridgeF6SC::bankCount() const
{
    return 4;
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
bool CartridgeF6SC::patch(uInt16 address, uInt8 value)
{
    address &= 0x0FFF;

    if (address < 0x0100)
    {
      // Normally, a write to the read port won't do anything
      // However, the patch command is special in that ignores such
      // cart restrictions
        myRAM[address & 0x007F] = value;
    }
    else
        myImage[(myCurrentBank << 12) + address] = value;

    return myBankChanged = true;
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
const uInt8* CartridgeF6SC::getImage(int& size) const
{
    size = 16384;
    return myImage;
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
bool CartridgeF6SC::save(Serializer& out) const
{
    try
    {
        out.putString(name());
        out.putShort(myCurrentBank);
        out.putByteArray(myRAM, 128);
    }
    catch (...)
    {
        cerr << "ERROR: CartridgeF6SC::save" << endl;
        return false;
    }

    return true;
}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
bool CartridgeF6SC::load(Serializer& in)
{
    try
    {
        if (in.getString() != name())
            return false;

        myCurrentBank = in.getShort();
        in.getByteArray(myRAM, 128);
    }
    catch (...)
    {
        cerr << "ERROR: CartridgeF6SC::load" << endl;
        return false;
    }

    // Remember what bank we were in
    bank(myCurrentBank);

    return true;
}