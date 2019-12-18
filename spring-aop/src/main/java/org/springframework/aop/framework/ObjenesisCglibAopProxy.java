package org.springframework.aop.framework;

public class ObjenesisCglibAopProxy extends CglibAopProxy{

    private static final long serialVersionUID = 1227139454465040612L;

    // private static final Log logger = LogFactory.getLog(ObjenesisCglibAopProxy.class);
    // private static final SpringObjenesis objenesis = new SpringObjenesis();

    public ObjenesisCglibAopProxy(AdvisedSupport config) {
        super(config);
    }

}
