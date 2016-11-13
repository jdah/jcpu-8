package net.jonathan.emulator;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;

public class PiKeyboardListener implements KeyListener
{

	private LinkedList<ExtendedKeyEvent> keyBuffer = new LinkedList<>();

	public static class ExtendedKeyEvent
	{
		public enum EventType
		{
			KEY_PRESSED,
			KEY_RELEASED
		}

		private KeyEvent e;
		private EventType type;

		public ExtendedKeyEvent(KeyEvent e, EventType type)
		{
			this.e = e;
			this.type = type;
		}

		public KeyEvent getEvent()
		{
			return e;
		}

		public EventType getType()
		{
			return type;
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{

	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		keyBuffer.add(new ExtendedKeyEvent(e, ExtendedKeyEvent.EventType.KEY_PRESSED));
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		keyBuffer.add(new ExtendedKeyEvent(e, ExtendedKeyEvent.EventType.KEY_RELEASED));
	}

	public ExtendedKeyEvent getNextEvent()
	{
		ExtendedKeyEvent toReturn = keyBuffer.getFirst();
		keyBuffer.removeFirst();
		return toReturn;
	}

	public boolean hasNext()
	{
		return keyBuffer.size() > 0;
	}

	public void clearBuffer()
	{
		keyBuffer.clear();
	}
}
