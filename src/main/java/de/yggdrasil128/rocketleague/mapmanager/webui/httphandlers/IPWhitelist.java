package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.sun.net.httpserver.HttpHandler;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class IPWhitelist {
	private Set<InetAddress> whitelist = Collections.emptySet();
	
	public void setWhitelist(Collection<InetAddress> whitelist) {
		if(whitelist == null) {
			this.whitelist = Collections.emptySet();
		} else {
			this.whitelist = new HashSet<>(whitelist);
		}
	}
	
	public HttpHandler forHttpHandler(HttpHandler httpHandler) {
		return exchange -> {
			if(!checkAddress(exchange.getRemoteAddress().getAddress())) {
				exchange.sendResponseHeaders(403, -1);
				exchange.getResponseBody().close();
				return;
			}
			
			httpHandler.handle(exchange);
		};
	}
	
	private boolean checkAddress(InetAddress address) {
		if(address.isLoopbackAddress()) {
			return true;
		}
		
		return whitelist.contains(address);
	}
}
