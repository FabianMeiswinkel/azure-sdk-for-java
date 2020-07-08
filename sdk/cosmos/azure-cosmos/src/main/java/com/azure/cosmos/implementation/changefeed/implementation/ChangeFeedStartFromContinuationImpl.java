package com.azure.cosmos.implementation.changefeed.implementation;

class ChangeFeedStartFromContinuationImpl extends ChangeFeedStartFromInternal {
    private final String continuation;

    public ChangeFeedStartFromContinuationImpl(String continuation) {
        super();

        if (continuation == null) {
            throw new NullPointerException("continuation");
        }

        this.continuation = continuation;
    }

    public String getContinuation() {
        return this.continuation;
    }

    @Override
    void accept(ChangeFeedStartFromVisitor visitor) {
        visitor.Visit(this);
    }
}

