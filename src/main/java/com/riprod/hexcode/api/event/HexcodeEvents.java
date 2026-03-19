package com.riprod.hexcode.api.event;

import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public class HexcodeEvents {

    @SuppressWarnings("unchecked")
    public static <T extends CancellableEcsEvent & IEvent<Void>> T dispatch(T event) {
        IEventDispatcher<T, T> dispatcher = HytaleServer.get().getEventBus()
                .<Void, T>dispatchFor((Class<? super T>) event.getClass());
        if (!dispatcher.hasListener()) return event;
        return dispatcher.dispatch(event);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IEvent<Void>> T fire(T event) {
        IEventDispatcher<T, T> dispatcher = HytaleServer.get().getEventBus()
                .<Void, T>dispatchFor((Class<? super T>) event.getClass());
        if (!dispatcher.hasListener()) return event;
        return dispatcher.dispatch(event);
    }
}
