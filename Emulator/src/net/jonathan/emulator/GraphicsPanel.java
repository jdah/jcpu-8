package net.jonathan.emulator;

import javax.swing.*;
import java.awt.*;

/**
 * User: Jonathan
 * Date: 7/23/2014
 * Time: 1:10 PM
 */
public class GraphicsPanel
{
	public static final int SCREEN_WIDTH = 320;
	public static final int SCREEN_HEIGHT = 240;

	public int translationX = 0;
	public int translationY = 0;

	private byte[][] pixels = new byte[SCREEN_WIDTH][SCREEN_HEIGHT];

	private JPanel graphicsPanel;

	private boolean isColorOverridden;
	private byte overrideColor;

	public GraphicsPanel()
	{
		graphicsPanel = new JPanel()
		{
			@Override
			public void paintComponent(Graphics g)
			{
				super.paintComponent(g);

				for(int x = 0; x < SCREEN_WIDTH; x++)
					for(int y = 0; y < SCREEN_HEIGHT; y++)
					{
						byte b = pixels[x][y];
						int red = (int) Math.round(((b & 0xE0) >>> 5) / 7.0 * 255.0);
						int green = (int) Math.round(((b & 0x1C) >>> 2) / 7.0 * 255.0);
						int blue = (int) Math.round((b & 0x03) / 3.0 * 255.0);
						g.setColor(new Color(red, green, blue));
						g.drawRect(x, y, 1, 1);
					}
			}
		};

		graphicsPanel.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
		graphicsPanel.setVisible(true);
	}

	public void overrideColor(byte color)
	{
		isColorOverridden = true;
		overrideColor = color;
	}

	public void stopOverrideColor()
	{
		isColorOverridden = false;
	}

	public void clear()
	{
		Graphics g = graphicsPanel.getGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, GraphicsPanel.SCREEN_WIDTH, GraphicsPanel.SCREEN_HEIGHT);
	}

	public JPanel getGraphicsPanel()
	{
		return graphicsPanel;
	}

	public void setPixel(byte color, int x, int y)
	{
		int realX = x + translationX;
		int realY = y + translationY;

		if(realX > -1 && realX < SCREEN_WIDTH && realY > -1 && realY < SCREEN_HEIGHT)
		{
			pixels[realX][realY] = isColorOverridden ? overrideColor : color;
			graphicsPanel.repaint();
		}
	}

	/* Move the memory a certain direction in pixels */
	public void moveMem(int x, int y)
	{
		/* TODO: Find a faster method of doing this */
		for(int i = 0; i < pixels.length; i++)
			for(int j = 0; j < pixels[0].length; j++)
				if(i + x > -1 && i + x < SCREEN_WIDTH && j + y > -1 && j + y < SCREEN_HEIGHT)
					pixels[i + x][j + y] = pixels[i][j];
				else
					pixels[i + x][j + y] = 0x00;
	}

}
