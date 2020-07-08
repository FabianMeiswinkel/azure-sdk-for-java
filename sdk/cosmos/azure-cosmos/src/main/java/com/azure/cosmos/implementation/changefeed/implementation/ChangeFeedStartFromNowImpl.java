package com.azure.cosmos.implementation.changefeed.implementation;

class ChangeFeedStartFromNowImpl extends ChangeFeedStartFromInternal {
    public ChangeFeedStartFromNowImpl() {
        super();
    }

    @Override
    void accept(ChangeFeedStartFromVisitor visitor) {
        visitor.Visit(this);
    }
}
