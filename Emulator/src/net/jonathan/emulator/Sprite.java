package net.jonathan.emulator;

import java.util.HashMap;
import java.util.HashSet;

public class Sprite
{

	public static class Pixel
	{
		private int x;
		private int y;
		private byte color;

		public Pixel(int x, int y, byte color)
		{
			this.x = x;
			this.y = y;
			this.color = color;
		}

		public byte getColor()
		{
			return color;
		}

		public int getY()
		{
			return y;
		}

		public int getX()
		{
			return x;
		}
	}

	private HashSet<Pixel> pixels = new HashSet<>();
	private GraphicsPanel panel;

	public Sprite(GraphicsPanel panel)
	{
		this.panel = panel;
	}

	public void draw()
	{
		for(Pixel pix : pixels)
			panel.setPixel(pix.getColor(), pix.getX(), pix.getY());
	}

	public void addPixel(Pixel p)
	{
		pixels.add(p);
	}

}
