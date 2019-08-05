package com.ciscowebex.androidsdk.people.internal;

import com.cisco.spark.android.model.conversation.Activity;
import com.ciscowebex.androidsdk.people.Person;

public class PersonImpl extends Person {

    public PersonImpl(Activity activity) {
        super(activity);
    }


    public PersonImpl(com.cisco.spark.android.model.Person actor) {
        super(actor);
    }

}
