package com.ciscowebex.androidsdk.internal;

import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.auth.*;
import com.ciscowebex.androidsdk.auth.internal.TokenAuthenticator;
import com.ciscowebex.androidsdk.membership.internal.MembershipClientImpl;
import com.ciscowebex.androidsdk.message.Message;
import com.ciscowebex.androidsdk.message.internal.CallbackablePostCommentOperation;
import com.ciscowebex.androidsdk.message.internal.CallbackablePostContentActivityOperation;
import com.ciscowebex.androidsdk.message.internal.MessageClientImpl;
import com.ciscowebex.androidsdk.message.internal.MessageMarkReadOperation;
import com.ciscowebex.androidsdk.phone.internal.CallImpl;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.ciscowebex.androidsdk.space.internal.SpaceClientImpl;
import com.ciscowebex.androidsdk_commlib.internal.SDKCommonComponent;
import dagger.Component;

@SDKScope
@Component(dependencies = SDKCommonComponent.class, modules = SDKModule.class)
public interface SDKComponent {

    Webex inject(Webex webex);

    JWTAuthenticator inject(JWTAuthenticator authenticator);

    OAuthAuthenticator inject(OAuthAuthenticator authenticator);

    OAuthWebViewAuthenticator inject(OAuthWebViewAuthenticator authenticator);

    OAuthTestUserAuthenticator inject(OAuthTestUserAuthenticator authenticator);

    SSOAuthenticator inject(SSOAuthenticator authenticator);

    TokenAuthenticator inject(TokenAuthenticator authenticator);

    PhoneImpl inject(PhoneImpl phone);

    CallImpl inject(CallImpl call);

    MessageClientImpl inject(MessageClientImpl client);

    MessageMarkReadOperation inject(MessageMarkReadOperation operation);

    CallbackablePostCommentOperation inject(CallbackablePostCommentOperation operation);

    CallbackablePostContentActivityOperation inject(CallbackablePostContentActivityOperation operation);

    MembershipClientImpl inject(MembershipClientImpl client);

    SpaceClientImpl inject(SpaceClientImpl client);
}
