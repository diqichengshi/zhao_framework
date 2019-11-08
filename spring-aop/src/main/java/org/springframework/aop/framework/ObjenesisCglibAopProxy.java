package org.springframework.aop.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.objenesis.SpringObjenesis;

public class ObjenesisCglibAopProxy extends CglibAopProxy{

    private static final Log logger = LogFactory.getLog(ObjenesisCglibAopProxy.class);
    private static final SpringObjenesis objenesis = new SpringObjenesis();

    public ObjenesisCglibAopProxy(AdvisedSupport config) {
        super(config);
    }

}
