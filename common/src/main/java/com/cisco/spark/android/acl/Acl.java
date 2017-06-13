package com.cisco.spark.android.acl;

import android.net.Uri;

import java.util.Date;
import java.util.Set;

public class Acl {

    private Uri url;
    private Uri peopleUrl;
    private Uri kmsResourceUrl;
    private Uri defaultEncryptionUrl;
    private Date lastModified;
    private Uri webhooksUrl;
    private Uri parent;
    private String kmsMessage;
    private Set<Tag> tags;
    private Set<AclLink> links;
    private Set<AclLinkResponse> aclLinks;

    public Acl() {

    }

    public Acl(Uri url, Uri peopleUrl, Uri kmsResourceUrl, Uri defaultEncryptionUrl, Date lastModified, Uri webhooksUrl,
               Uri parent, String kmsMessage, Set<Tag> tags, Set<AclLink> links, Set<AclLinkResponse> aclLinks) {
        this.url = url;
        this.peopleUrl = peopleUrl;
        this.kmsResourceUrl = kmsResourceUrl;
        this.defaultEncryptionUrl = defaultEncryptionUrl;
        this.lastModified = lastModified;
        this.webhooksUrl = webhooksUrl;
        this.parent = parent;
        this.kmsMessage = kmsMessage;
        this.tags = tags;
        this.links = links;
        this.aclLinks = aclLinks;
    }

    public Uri getKmsResourceUrl() {
        return kmsResourceUrl;
    }

    public static class Builder {

        private Uri mUrl;
        private Uri mPeopleUrl;
        private Uri mKmsResourceUrl;
        private Uri mDefaultEncryptionUrl;
        private Date mLastModified;
        private Uri mWebhooksUrl;
        private Uri mParent;
        private String mKmsMessage;
        private Set<Tag> mTags;
        private Set<AclLink> mLinks;
        private Set<AclLinkResponse> mAclLinks;

        public Builder setUrl(Uri url) {
            mUrl = url;
            return this;
        }

        public Builder setPeopleUrl(Uri peopleUrl) {
            mPeopleUrl = peopleUrl;
            return this;
        }

        public Builder setKmsResourceUrl(Uri kmsResourceUrl) {
            mKmsResourceUrl = kmsResourceUrl;
            return this;
        }

        public Builder setDefaultEncryptionUrl(Uri defaultEncryptionUrl) {
            mDefaultEncryptionUrl = defaultEncryptionUrl;
            return this;
        }

        public Builder setLastModified(Date lastModified) {
            mLastModified = lastModified;
            return this;
        }

        public Builder setWebhooksUrl(Uri webhooksUrl) {
            mWebhooksUrl = webhooksUrl;
            return this;
        }

        public Builder setParent(Uri parent) {
            mParent = parent;
            return this;
        }

        public Builder setKmsMessage(String kmsMessage) {
            mKmsMessage = kmsMessage;
            return this;
        }

        public Builder setTags(Set<Tag> tags) {
            mTags = tags;
            return this;
        }

        public Builder setLinks(Set<AclLink> links) {
            mLinks = links;
            return this;
        }

        public Builder setAclLinks(Set<AclLinkResponse> aclLinks) {
            mAclLinks = aclLinks;
            return this;
        }

        public Acl createAcl() {
            return new Acl(mUrl, mPeopleUrl, mKmsResourceUrl, mDefaultEncryptionUrl, mLastModified, mWebhooksUrl, mParent,
                           mKmsMessage, mTags, mLinks, mAclLinks);
        }
    }
}
