/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.util;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.res.StringManager;

/**
 * Base implementation of the {@link Lifecycle} interface that implements the
 * state transition rules for {@link Lifecycle#start()} and
 * {@link Lifecycle#stop()}
 */
public abstract class LifecycleBase implements Lifecycle {

	private static Log log = LogFactory.getLog(LifecycleBase.class);

	private static StringManager sm = StringManager.getManager("org.apache.catalina.util");

	/**
	 * Used to handle firing lifecycle events. TODO: Consider merging
	 * LifecycleSupport into this class.
	 */
	private LifecycleSupport lifecycle = new LifecycleSupport(this);

	/**
	 * The current state of the source component.
	 */
	private volatile LifecycleState state = LifecycleState.NEW;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		lifecycle.addLifecycleListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return lifecycle.findLifecycleListeners();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		lifecycle.removeLifecycleListener(listener);
	}

	/**
	 * Allow sub classes to fire {@link Lifecycle} events.
	 * 
	 * @param type Event type
	 * @param data Data associated with event.
	 */
	protected void fireLifecycleEvent(String type, Object data) {
		lifecycle.fireLifecycleEvent(type, data);
	}

	@Override
	public final synchronized void init() throws LifecycleException {
		// 状态不为new,抛异常
		if (!state.equals(LifecycleState.NEW)) {
			invalidTransition(Lifecycle.BEFORE_INIT_EVENT);
		}
		// 初始化逻辑之前,设置状态为INITIALIZING
		setStateInternal(LifecycleState.INITIALIZING, null, false);

		try {
			// 初始化,该方法为一个abstract方法,需要组件自行实现
			initInternal();
		} catch (Throwable t) {
			// 初始化的过程中,可能会有异常抛出,这时需要捕获异常,并将状态变更为FAILED
			ExceptionUtils.handleThrowable(t);
			setStateInternal(LifecycleState.FAILED, null, false);
			throw new LifecycleException(sm.getString("lifecycleBase.initFail", toString()), t);
		}
		// 初始化完成之后,状态变更为INITIALIZED
		setStateInternal(LifecycleState.INITIALIZED, null, false);
	}

	/**
	 * 使用状态机+模板模式来实现初始化,此处为模板方法
	 */
	protected abstract void initInternal() throws LifecycleException;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final synchronized void start() throws LifecycleException {
		// STARTING_PREP STARTING和STARTED时,将忽略start()逻辑
		if (LifecycleState.STARTING_PREP.equals(state) || LifecycleState.STARTING.equals(state)
				|| LifecycleState.STARTED.equals(state)) {

			if (log.isDebugEnabled()) {
				Exception e = new LifecycleException();
				log.debug(sm.getString("lifecycleBase.alreadyStarted", toString()), e);
			} else if (log.isInfoEnabled()) {
				log.info(sm.getString("lifecycleBase.alreadyStarted", toString()));
			}

			return;
		}
		// NEW状态时,执行init()方法
		if (state.equals(LifecycleState.NEW)) {
			// init
			init();
			// FAILED状态时,执行stop()方法
		} else if (state.equals(LifecycleState.FAILED)) {
			// stop
			stop();
			// 不是INITIALIZED和STOPPED时,则说明是非法的操作
		} else if (!state.equals(LifecycleState.INITIALIZED) && !state.equals(LifecycleState.STOPPED)) {
			invalidTransition(Lifecycle.BEFORE_START_EVENT);
		}

		// start前的状态设置
		setStateInternal(LifecycleState.STARTING_PREP, null, false);

		try {
			// start逻辑,抽象方法,由组件自行实现
			startInternal();
		} catch (Throwable t) {
			ExceptionUtils.handleThrowable(t);
			setStateInternal(LifecycleState.FAILED, null, false);
			throw new LifecycleException(sm.getString("lifecycleBase.startFail", toString()), t);
		}

		// start过程中,可能因为某些原因失败,这时需要stop操作
		if (state.equals(LifecycleState.FAILED) || state.equals(LifecycleState.MUST_STOP)) {
			stop();
		} else {
			// Shouldn't be necessary but acts as a check that sub-classes are
			// doing what they are supposed to.
			if (!state.equals(LifecycleState.STARTING)) {
				invalidTransition(Lifecycle.AFTER_START_EVENT);
			}
			// 设置状态为STARTED
			setStateInternal(LifecycleState.STARTED, null, false);
		}
	}

	/**
	 * 使用状态机+模板模式来实现启动,此处为模板方法 Sub-classes must ensure that the state is changed to
	 * {@link LifecycleState#STARTING} during the execution of this method. Changing
	 * state will trigger the {@link Lifecycle#START_EVENT} event.
	 * 
	 * If a component fails to start it may either throw a
	 * {@link LifecycleException} which will cause it's parent to fail to start or
	 * it can place itself in the error state in which case {@link #stop()} will be
	 * called on the failed component but the parent component will continue to
	 * start normally.
	 * 
	 * @throws LifecycleException
	 */
	protected abstract void startInternal() throws LifecycleException;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final synchronized void stop() throws LifecycleException {
		// STOPPING_PREP STOPPING 和STOPPED时,将忽略stop()的执行
		if (LifecycleState.STOPPING_PREP.equals(state) || LifecycleState.STOPPING.equals(state)
				|| LifecycleState.STOPPED.equals(state)) {

			if (log.isDebugEnabled()) {
				Exception e = new LifecycleException();
				log.debug(sm.getString("lifecycleBase.alreadyStopped", toString()), e);
			} else if (log.isInfoEnabled()) {
				log.info(sm.getString("lifecycleBase.alreadyStopped", toString()));
			}

			return;
		}

		// NEW状态时，直接将状态变更为STOPPED
		if (state.equals(LifecycleState.NEW)) {
			state = LifecycleState.STOPPED;
			return;
		}

		// stop()的执行,必须要是STARTED和FAILED
		if (!state.equals(LifecycleState.STARTED) && !state.equals(LifecycleState.FAILED)
				&& !state.equals(LifecycleState.MUST_STOP)) {
			invalidTransition(Lifecycle.BEFORE_STOP_EVENT);
		}

		// FAILED时,直接触发BEFORE_STOP_EVENT事件
		if (state.equals(LifecycleState.FAILED)) {
			// Don't transition to STOPPING_PREP as that would briefly mark the
			// component as available but do ensure the BEFORE_STOP_EVENT is
			// fired
			fireLifecycleEvent(BEFORE_STOP_EVENT, null);
		} else {
			// 设置状态为STOPPING_PREP
			setStateInternal(LifecycleState.STOPPING_PREP, null, false);
		}

		try {
			// stop逻辑,抽象方法,组件自行实现
			stopInternal();
		} catch (Throwable t) {
			ExceptionUtils.handleThrowable(t);
			setStateInternal(LifecycleState.FAILED, null, false);
			throw new LifecycleException(sm.getString("lifecycleBase.stopFail", toString()), t);
		}

		if (state.equals(LifecycleState.MUST_DESTROY)) {
			// Complete stop process first
			setStateInternal(LifecycleState.STOPPED, null, false);

			destroy();
		} else {
			// Shouldn't be necessary but acts as a check that sub-classes are
			// doing what they are supposed to.
			if (!state.equals(LifecycleState.STOPPING)) {
				invalidTransition(Lifecycle.AFTER_STOP_EVENT);
			}
			// 设置状态为STOPPED
			setStateInternal(LifecycleState.STOPPED, null, false);
		}
	}

	/**
	 * 使用状态机+模板模式来实现关闭,此处为模板方法 Sub-classes must ensure that the state is changed to
	 * {@link LifecycleState#STOPPING} during the execution of this method. Changing
	 * state will trigger the {@link Lifecycle#STOP_EVENT} event.
	 * 
	 * @throws LifecycleException
	 */
	protected abstract void stopInternal() throws LifecycleException;

	@Override
	public final synchronized void destroy() throws LifecycleException {
		// FAILED状态时,直接触发stop()逻辑
		if (LifecycleState.FAILED.equals(state)) {
			try {
				// Triggers clean-up
				stop();
			} catch (LifecycleException e) {
				// Just log. Still want to destroy.
				log.warn(sm.getString("lifecycleBase.destroyStopFail"), e);
			}
		}

		// DESTROYING和DESTROYED时,忽略destroy的执行
		if (LifecycleState.DESTROYING.equals(state) || LifecycleState.DESTROYED.equals(state)) {

			if (log.isDebugEnabled()) {
				Exception e = new LifecycleException();
				log.debug(sm.getString("lifecycleBase.alreadyDestroyed", toString()), e);
			} else if (log.isInfoEnabled()) {
				log.info(sm.getString("lifecycleBase.alreadyDestroyed", toString()));
			}

			return;
		}

		// 非法状态判断
		if (!state.equals(LifecycleState.STOPPED) && !state.equals(LifecycleState.FAILED)
				&& !state.equals(LifecycleState.NEW) && !state.equals(LifecycleState.INITIALIZED)) {
			invalidTransition(Lifecycle.BEFORE_DESTROY_EVENT);
		}

		// destroy前状态设置
		setStateInternal(LifecycleState.DESTROYING, null, false);

		try {
			// 抽象方法,组件自行实现
			destroyInternal();
		} catch (Throwable t) {
			ExceptionUtils.handleThrowable(t);
			setStateInternal(LifecycleState.FAILED, null, false);
			throw new LifecycleException(sm.getString("lifecycleBase.destroyFail", toString()), t);
		}
		// destroy后状态设置
		setStateInternal(LifecycleState.DESTROYED, null, false);
	}

	/**
	 * 使用状态机+模板模式来实现销毁,此处为模板方法
	 *
	 * @throws LifecycleException
	 */
	protected abstract void destroyInternal() throws LifecycleException;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LifecycleState getState() {
		return state;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getStateName() {
		return getState().toString();
	}

	/**
	 * Provides a mechanism for sub-classes to update the component state. Calling
	 * this method will automatically fire any associated {@link Lifecycle} event.
	 * It will also check that any attempted state transition is valid for a
	 * sub-class.
	 * 
	 * @param state The new state for this component
	 */
	protected synchronized void setState(LifecycleState state) throws LifecycleException {
		setStateInternal(state, null, true);
	}

	/**
	 * Provides a mechanism for sub-classes to update the component state. Calling
	 * this method will automatically fire any associated {@link Lifecycle} event.
	 * It will also check that any attempted state transition is valid for a
	 * sub-class.
	 * 
	 * @param state The new state for this component
	 * @param data  The data to pass to the associated {@link Lifecycle} event
	 */
	protected synchronized void setState(LifecycleState state, Object data) throws LifecycleException {
		setStateInternal(state, data, true);
	}

	private synchronized void setStateInternal(LifecycleState state, Object data, boolean check)
			throws LifecycleException {

		if (log.isDebugEnabled()) {
			log.debug(sm.getString("lifecycleBase.setState", this, state));
		}

		// 是否校验状态
		if (check) {
			// Must have been triggered by one of the abstract methods (assume
			// code in this class is correct)
			// null is never a valid state
			// state不允许为null
			if (state == null) {
				invalidTransition("null");
				// Unreachable code - here to stop eclipse complaining about
				// a possible NPE further down the method
				return;
			}

			// Any method can transition to failed
			// startInternal() permits STARTING_PREP to STARTING
			// stopInternal() permits STOPPING_PREP to STOPPING and FAILED to
			// STOPPING
			if (!(state == LifecycleState.FAILED
					|| (this.state == LifecycleState.STARTING_PREP && state == LifecycleState.STARTING)
					|| (this.state == LifecycleState.STOPPING_PREP && state == LifecycleState.STOPPING)
					|| (this.state == LifecycleState.FAILED && state == LifecycleState.STOPPING))) {
				// No other transition permitted
				invalidTransition(state.name());
			}
		}

		// 设置状态
		this.state = state;
		// 触发事件
		String lifecycleEvent = state.getLifecycleEvent();
		if (lifecycleEvent != null) {
			fireLifecycleEvent(lifecycleEvent, data);
		}
	}

	private void invalidTransition(String type) throws LifecycleException {
		String msg = sm.getString("lifecycleBase.invalidTransition", type, toString(), state);
		throw new LifecycleException(msg);
	}
}
