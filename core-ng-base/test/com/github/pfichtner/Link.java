package com.github.pfichtner;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.pfichtner.Connection.Listener;
import com.github.pfichtner.Pin.AnalogPin;
import com.github.pfichtner.Pin.DigitalPin;
import com.github.pfichtner.events.AnalogPinValueChangedEvent;
import com.github.pfichtner.events.DefaultAnalogPinValueChangedEvent;
import com.github.pfichtner.events.DefaultDigitalPinValueChangedEvent;
import com.github.pfichtner.events.DigitalPinValueChangedEvent;
import com.github.pfichtner.events.EventListener;
import com.github.pfichtner.proto.api.Protocol;
import com.github.pfichtner.proto.api.Protocol.FromArduino;
import com.github.pfichtner.proto.impl.DefaultToArduino;

public class Link {

	private final Connection connection;
	private final Protocol protocol;
	private final List<EventListener> listeners = new CopyOnWriteArrayList<EventListener>();

	public Link(Connection connection, Protocol protocol) {
		this.connection = connection;
		this.protocol = protocol;
		this.connection.setListener(new Listener() {
			@Override
			public void received(byte[] bytes) throws IOException {
				Link.this.received(bytes);
			}
		});
	}

	public Link addListener(EventListener listener) {
		this.listeners.add(listener);
		return this;
	}

	public void switchAnalogPin(AnalogPin analogPin, Integer value)
			throws IOException {
		send(analogPin, value);
	}

	public void switchDigitalPin(DigitalPin digitalPin, Boolean value)
			throws IOException {
		send(digitalPin, value);
	}

	private void send(Pin pin, Object value) throws IOException {
		this.connection.write(this.protocol.toArduino(new DefaultToArduino(pin,
				value)));
	}

	protected void received(byte[] bytes) {
		FromArduino fromArduino = this.protocol.fromArduino(bytes);
		Pin pin = fromArduino.getPin();
		Object value = fromArduino.getValue();
		if (pin instanceof AnalogPin && value instanceof Integer) {
			AnalogPinValueChangedEvent event = new DefaultAnalogPinValueChangedEvent(
					(AnalogPin) pin, (Integer) value);
			for (EventListener eventListener : this.listeners) {
				eventListener.stateChanged(event);
			}
		}
		if (pin instanceof DigitalPin && value instanceof Boolean) {
			DigitalPinValueChangedEvent event = new DefaultDigitalPinValueChangedEvent(
					(DigitalPin) pin, (Boolean) value);
			for (EventListener eventListener : this.listeners) {
				eventListener.stateChanged(event);
			}
		}
	}

}
