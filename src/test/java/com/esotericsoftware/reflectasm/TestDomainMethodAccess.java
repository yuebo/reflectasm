package com.esotericsoftware.reflectasm;

import java.util.function.BiFunction;

public class TestDomainMethodAccess extends MethodAccess {
    private BiFunction<Object, Object[],Object>[] functions=new BiFunction[10];

    public TestDomainMethodAccess(){
        functions[0]=(object,param)->{
          TestDomain source=(TestDomain)object;
          return source.getField1();
        };

        functions[1]=(object,param)->{
            TestDomain source=(TestDomain)object;
            source.setField1((String)param[0]);
            return null;
        };
        functions[2]=(object,param)->{
            TestDomain source=(TestDomain)object;
            return source.getField2();
        };

        functions[3]=(object,param)->{
            TestDomain source=(TestDomain)object;
            source.setField2((String)param[0]);
            return null;
        };

        functions[5]=(object,param)->{
            TestDomain source=(TestDomain)object;
            return source.getField3();
        };

        functions[6]=(object,param)->{
            TestDomain source=(TestDomain)object;
            source.setField3((String)param[0]);
            return null;
        };


    }

    @Override
    public Object invoke(Object object, int methodIndex, Object... args) {
        if (methodIndex>=functions.length){
            throw new IllegalArgumentException("no such method");
        }
        return functions[methodIndex].apply(object,args);
    }
}
