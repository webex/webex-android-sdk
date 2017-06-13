package com.cisco.spark.android.sync.operationqueue;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.sync.KmsResourceObject;

public class TestAddPersonOperation extends AddPersonOperation {
    private KmsResourceObject kmsResourceObject;

    public TestAddPersonOperation(Injector injector, String conversationId, Person person, boolean isTeam, KmsResourceObject kmsResourceObject) {
        super(injector, conversationId, person, isTeam);
        this.kmsResourceObject = kmsResourceObject;
    }

    @Override
    protected KmsResourceObject getKmsResourceObject() {
        return kmsResourceObject;
    }
}
