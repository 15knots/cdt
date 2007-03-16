/*******************************************************************************
 * Copyright (c) 2006-2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ted R Williams (Wind River Systems, Inc.) - initial implementation
 *******************************************************************************/


package org.eclipse.dd.debug.memory.renderings.traditional;

import java.math.BigInteger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

public class DataPane extends AbstractPane
{
    public DataPane(Rendering parent)
    {
        super(parent);
    }

    protected String getCellText(MemoryByte bytes[])
    {
        return fRendering.getRadixText(bytes, fRendering.getRadix(), fRendering
            .isLittleEndian());
    }

    protected void editCell(BigInteger address, int subCellPosition,
        char character)
    {
        try
        {
            MemoryByte bytes[] = fRendering.getBytes(fCaretAddress, fRendering
                .getBytesPerColumn());
          
            String cellText = getCellText(bytes);
            if(cellText == null)
                return;
            
            StringBuffer cellTextBuffer = new StringBuffer(cellText);
            cellTextBuffer.setCharAt(subCellPosition, character);
            BigInteger value = new BigInteger(cellTextBuffer.toString().trim(),
                fRendering.getNumericRadix(fRendering.getRadix()));
            final boolean isSignedType = fRendering.getRadix() == Rendering.RADIX_DECIMAL_SIGNED;
            final boolean isSigned = isSignedType
                && value.compareTo(BigInteger.valueOf(0)) < 0;

            int bitCount = value.bitLength();
            if(isSignedType)
                bitCount++;
            if(bitCount > fRendering.getBytesPerColumn() * 8)
                return;

            int byteLen = fRendering.getBytesPerColumn();
            byte[] byteData = new byte[byteLen];
            for(int i = 0; i < byteLen; i++)
            {
                int bits = 255;
                if(isSignedType && i == byteLen - 1)
                    bits = 127;

                byteData[i] = (byte) (value.and(BigInteger.valueOf(bits))
                    .intValue() & bits);
                value = value.shiftRight(8);
            }

            if(isSigned)
                byteData[byteLen - 1] |= 128;

            boolean shouldReorderBytes = fRendering.isLittleEndian() == bytes[0].isBigEndian(); // swapped in presentation
            if(!bytes[0].isBigEndian()) // swapped by BigInteger/java endianness
            	shouldReorderBytes = !shouldReorderBytes;
            
            if(shouldReorderBytes)
            {
                byte[] byteDataSwapped = new byte[byteData.length];
                for(int i = 0; i < byteData.length; i++)
                    byteDataSwapped[i] = byteData[byteData.length - 1 - i];
                byteData = byteDataSwapped;
            }

            if(byteData.length != bytes.length)
                return;

            TraditionalMemoryByte bytesToSet[] = new TraditionalMemoryByte[bytes.length];

            for(int i = 0; i < byteData.length; i++)
            {
                bytesToSet[i] = new TraditionalMemoryByte(byteData[i]);
                bytesToSet[i].setBigEndian(bytes[i].isBigEndian());

                if(bytes[i].getValue() != byteData[i])
                {
                    bytesToSet[i].setEdited(true);
                }
                else
                {
                    bytesToSet[i].setChanged(bytes[i].isChanged());
                }
            }
            
            fRendering.getViewportCache().setEditedValue(address, bytesToSet);

            advanceCursor();

            redraw();
        }
        catch(Exception e)
        {
            // do nothing
        }
    }

    protected int getCellWidth()
    {
        return getCellCharacterCount() * getCellCharacterWidth()
            + (fRendering.getCellPadding() * 2);
    }

    protected int getCellCharacterCount()
    {
        return fRendering.getRadixCharacterCount(fRendering.getRadix(),
            fRendering.getBytesPerColumn());
    }

    public Point computeSize(int wHint, int hHint)
    {
        return new Point(fRendering.getColumnCount() * getCellWidth()
            + fRendering.getRenderSpacing(), 100);
    }

    private BigInteger getCellAddressAt(int x, int y) throws DebugException
    {
        BigInteger address = fRendering.getViewportStartAddress();

        int col = x / getCellWidth();
        int row = y / getCellHeight();

        if(col >= fRendering.getColumnCount())
            return null;

        address = address.add(BigInteger.valueOf(row
            * fRendering.getColumnCount() * fRendering.getAddressesPerColumn()));

        address = address.add(BigInteger.valueOf(col
            * fRendering.getAddressesPerColumn()));

        return address;
    }

    protected Point getCellLocation(BigInteger cellAddress)
    {
        try
        {
            BigInteger address = fRendering.getViewportStartAddress();

            int cellOffset = cellAddress.subtract(address).intValue();
            cellOffset *= fRendering.getAddressableSize();

            int row = cellOffset
                / (fRendering.getColumnCount() * fRendering.getBytesPerColumn());
            cellOffset -= row * fRendering.getColumnCount()
                * fRendering.getBytesPerColumn();

            int col = cellOffset / fRendering.getBytesPerColumn();

            int x = col * getCellWidth() + fRendering.getCellPadding();
            int y = row * getCellHeight() + fRendering.getCellPadding();

            return new Point(x, y);
        }
        catch(Exception e)
        {
            fRendering
                .logError(
                    TraditionalRenderingMessages
                        .getString("TraditionalRendering.FAILURE_DETERMINE_CELL_LOCATION"), e); //$NON-NLS-1$
            return null;
        }
    }

    protected void positionCaret(int x, int y)
    {
        try
        {
            BigInteger cellAddress = getCellAddressAt(x, y);
            if(cellAddress != null)
            {
                Point cellPosition = getCellLocation(cellAddress);
                int offset = x - cellPosition.x;
                int subCellCharacterPosition = offset / getCellCharacterWidth();

                if(subCellCharacterPosition == this.getCellCharacterCount())
                {
                    cellAddress = cellAddress.add(BigInteger.valueOf(fRendering
                        .getAddressesPerColumn()));
                    subCellCharacterPosition = 0;
                    cellPosition = getCellLocation(cellAddress);
                }

                fCaret.setLocation(cellPosition.x + subCellCharacterPosition
                    * getCellCharacterWidth(), cellPosition.y);

                this.fCaretAddress = cellAddress;
                this.fSubCellCaretPosition = subCellCharacterPosition;
            }
        }
        catch(Exception e)
        {
            fRendering
                .logError(
                    TraditionalRenderingMessages
                        .getString("TraditionalRendering.FAILURE_POSITION_CURSOR"), e); //$NON-NLS-1$
        }
    }

    protected BigInteger getViewportAddress(int col, int row)
        throws DebugException
    {
        BigInteger address = fRendering.getViewportStartAddress();
        address = address.add(BigInteger.valueOf((row
            * fRendering.getColumnCount() + col)
            * fRendering.getAddressesPerColumn()));

        return address;
    }

    protected void paint(PaintEvent pe)
    {
        super.paint(pe);

        GC gc = pe.gc;
        gc.setFont(fRendering.getFont());

        int cellHeight = getCellHeight();
        int cellWidth = getCellWidth();

        int columns = fRendering.getColumnCount();

        try
        {
            BigInteger start = fRendering.getViewportStartAddress();

            for(int i = 0; i < this.getBounds().height / cellHeight; i++)
            {
                for(int col = 0; col < columns; col++)
                {
                	if(isOdd(col))
                		gc.setForeground(fRendering.getTraditionalRendering().getColorText());
                	else
                		gc.setForeground(fRendering.getTraditionalRendering().getColorTextAlternate());

                    BigInteger cellAddress = start.add(BigInteger.valueOf((i
                        * fRendering.getColumnCount() + col)
                        * fRendering.getAddressesPerColumn()));

                    MemoryByte bytes[] = fRendering.getBytes(cellAddress,
                        fRendering.getBytesPerColumn());

                    if(fRendering.getSelection().isSelected(cellAddress))
                    {
                        gc.setBackground(fRendering.getTraditionalRendering().getColorSelection());
                        gc.fillRectangle(cellWidth * col
                            + fRendering.getCellPadding(), cellHeight * i,
                            cellWidth, cellHeight);

                        gc.setForeground(fRendering.getTraditionalRendering().getColorBackground());
                    }
                    else
                    {
                        gc.setBackground(fRendering.getTraditionalRendering().getColorBackground());
                        gc.fillRectangle(cellWidth * col
                            + fRendering.getCellPadding(), cellHeight * i,
                            cellWidth, cellHeight);

                        // TODO consider adding finer granularity?
                        boolean anyByteChanged = false;
                        for(int n = 0; n < bytes.length && !anyByteChanged; n++)
                            if(bytes[n].isChanged())
                                anyByteChanged = true;
                        
                        // TODO consider adding finer granularity?
                        boolean anyByteEditing = false;
                        for(int n = 0; n < bytes.length && !anyByteEditing; n++)
                        	if(bytes[n] instanceof TraditionalMemoryByte)
                        		if(((TraditionalMemoryByte) bytes[n]).isEdited())
                        			anyByteEditing = true;
                        
                        if(anyByteEditing)
                        	gc.setForeground(fRendering.getTraditionalRendering().getColorEdit());
                        else if(anyByteChanged)
                        	gc.setForeground(fRendering.getTraditionalRendering().getColorChanged());
                        else if(isOdd(col))
                    		gc.setForeground(fRendering.getTraditionalRendering().getColorText());
                    	else
                    		gc.setForeground(fRendering.getTraditionalRendering().getColorTextAlternate());
                        
                        gc.setBackground(fRendering.getTraditionalRendering().getColorBackground());
                    }

                    gc.drawText(getCellText(bytes), cellWidth * col
                        + fRendering.getCellPadding(), cellHeight * i
                        + fRendering.getCellPadding());

                    BigInteger cellEndAddress = cellAddress.add(BigInteger
                        .valueOf(fRendering.getAddressesPerColumn()));
                    cellEndAddress = cellEndAddress.subtract(BigInteger
                        .valueOf(1));

                    if(fCaretEnabled)
                    {
                        if(cellAddress.compareTo(fCaretAddress) <= 0
                            && cellEndAddress.compareTo(fCaretAddress) >= 0)
                        {
                            int x = cellWidth * col
                                + fRendering.getCellPadding()
                                + fSubCellCaretPosition
                                * this.getCellCharacterWidth();
                            int y = cellHeight * i
                                + fRendering.getCellPadding();
                            fCaret.setLocation(x, y);
                        }
                    }

                    if(fRendering.isDebug())
                        gc.drawRectangle(cellWidth * col
                            + fRendering.getCellPadding(), cellHeight * i
                            + fRendering.getCellPadding(), cellWidth,
                            cellHeight);
                }
            }
        }
        catch(Exception e)
        {
            fRendering.logError(TraditionalRenderingMessages
                .getString("TraditionalRendering.FAILURE_PAINT"), e); //$NON-NLS-1$
        }

    }

}
