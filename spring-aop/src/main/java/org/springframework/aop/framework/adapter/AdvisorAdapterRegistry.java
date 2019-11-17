package org.springframework.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;

/**
 * Interface for registries of Advisor adapters.
 *
 * <p><i>This is an SPI interface, not to be implemented by any Spring user.</i>
 *
 * @author Rod Johnson
 * @author Rob Harrop
 */
public interface AdvisorAdapterRegistry {

	Advisor wrap(Object advice) throws UnknownAdviceTypeException;


	MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException;

	void registerAdvisorAdapter(AdvisorAdapter adapter);

}
