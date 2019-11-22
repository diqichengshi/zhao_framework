/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded.jetty;

import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Jetty {@link WebAppContext} used by {@link JettyEmbeddedServletContainer} to support
 * deferred initialization.
 *
 * @author Phillip Webb
 */
class JettyEmbeddedWebAppContext extends WebAppContext {

	@Override
	protected ServletHandler newServletHandler() {
		return new JettyEmbeddedServletHandler();
	}

	public void deferredInitialize() throws Exception {
		((JettyEmbeddedServletHandler) getServletHandler()).deferredInitialize();
	}

	private static class JettyEmbeddedServletHandler extends ServletHandler {

		@Override
		public void initialize() throws Exception {
		}

		public void deferredInitialize() throws Exception {
			super.initialize();
		}

	}

}
