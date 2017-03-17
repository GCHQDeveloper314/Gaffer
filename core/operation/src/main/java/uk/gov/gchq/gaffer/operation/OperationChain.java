/*
 * Copyright 2016-2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.gaffer.operation;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.operation.io.Input;
import uk.gov.gchq.gaffer.operation.io.InputIterableOutput;
import uk.gov.gchq.gaffer.operation.io.InputOutput;
import uk.gov.gchq.gaffer.operation.io.IterableInput;
import uk.gov.gchq.gaffer.operation.io.IterableInputIterableOutput;
import uk.gov.gchq.gaffer.operation.io.IterableInputOutput;
import uk.gov.gchq.gaffer.operation.io.IterableOutput;
import uk.gov.gchq.gaffer.operation.io.Output;
import uk.gov.gchq.gaffer.operation.io.InputOutputT;
import uk.gov.gchq.gaffer.operation.serialisation.TypeReferenceImpl;
import java.util.ArrayList;
import java.util.List;

/**
 * An <code>OperationChain</code> holds a list of {@link uk.gov.gchq.gaffer.operation.Operation}s that are chained together -
 * ie. the output of one operation is passed to the input of the next. For the chaining to be successful the operations
 * must be ordered correctly so the O and I types are compatible. The safest way to ensure they will be
 * compatible is to use the OperationChain.Builder to construct the chain.
 * <p>
 * A couple of special cases:
 * <ul>
 * <li>An operation with no output can come before any operation.</li>
 * <li>An operation with no input can follow any operation - the output from the previous operation will
 * just be lost.</li>
 * </ul>
 *
 * @param <OUT> the output type of the <code>OperationChain</code>. This should match the output type of the last
 *              {@link uk.gov.gchq.gaffer.operation.Operation} in the chain.
 * @see uk.gov.gchq.gaffer.operation.OperationChain.Builder
 */
public class OperationChain<OUT> {
    private List<Operation> operations;

    public OperationChain() {
    }

    public OperationChain(final Operation operation) {
        this(new ArrayList<>(1));
        operations.add(operation);
    }

    public OperationChain(final Output<OUT> operation) {
        this(new ArrayList<>(1));
        operations.add(operation);
    }

    public OperationChain(final List<Operation> operations) {
        this.operations = new ArrayList<>(operations);
    }

    @JsonIgnore
    public TypeReference<OUT> getOutputTypeReference() {
        if (null != operations && !operations.isEmpty()) {
            final Operation lastOp = operations.get(operations.size() - 1);
            if (lastOp instanceof Output) {
                return ((Output) lastOp).getOutputTypeReference();
            }
        }

        return (TypeReference<OUT>) new TypeReferenceImpl.Void();
    }

    public List<Operation> getOperations() {
        return operations;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
    @JsonGetter("operations")
    Operation[] getOperationArray() {
        return null != operations ? operations.toArray(new Operation[operations.size()]) : new Operation[0];
    }

    @JsonSetter("operations")
    void setOperationArray(final Operation[] operations) {
        if (null != operations) {
            this.operations = Lists.newArrayList(operations);
        } else {
            this.operations = null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder strBuilder = new StringBuilder("OperationChain[");

        if (null != operations) {
            boolean first = true;
            for (final Operation op : operations) {
                if (first) {
                    first = false;
                } else {
                    strBuilder.append("->");
                }

                strBuilder.append(op.getClass().getSimpleName());
            }
        }

        strBuilder.append("]");
        return strBuilder.toString();
    }

    /**
     * A <code>Builder</code> is a type safe way of building an {@link uk.gov.gchq.gaffer.operation.OperationChain}.
     * The builder instance is updated after each method call so it is best to chain the method calls together.
     * Usage:<br>
     * new Builder()<br>
     * &nbsp;.first(new SomeOperation.Builder()<br>
     * &nbsp;&nbsp;.addSomething()<br>
     * &nbsp;&nbsp;.build()<br>
     * &nbsp;)<br>
     * &nbsp;.then(new SomeOtherOperation.Builder()<br>
     * &nbsp;&nbsp;.addSomethingElse()<br>
     * &nbsp;&nbsp;.build()<br>
     * &nbsp;)<br>
     * &nbsp;.build();
     * <p>
     * For a full example see the Example module.
     */
    public static class Builder {
        public NoOutputBuilder first(final Operation op) {
            return new NoOutputBuilder(op);
        }

        public <NEXT_OUT> OutputBuilder<NEXT_OUT> first(final Output<NEXT_OUT> op) {
            return new OutputBuilder<>(op);
        }

        public <NEXT_OUT_ITEM> IterableOutputBuilder<NEXT_OUT_ITEM> first(final IterableOutput<NEXT_OUT_ITEM> op) {
            return new IterableOutputBuilder<>(op);
        }
    }

    public static final class NoOutputBuilder {
        private final List<Operation> ops;

        private NoOutputBuilder(final Operation op) {
            this(new ArrayList<>());
            ops.add(op);
        }

        private NoOutputBuilder(final List<Operation> ops) {
            this.ops = ops;
        }

        public NoOutputBuilder then(final Operation op) {
            ops.add(op);
            return new NoOutputBuilder(ops);
        }

        public <NEXT_OUT> OutputBuilder<NEXT_OUT> then(final Output<NEXT_OUT> op) {
            ops.add(op);
            return new OutputBuilder<>(ops);
        }

        public <NEXT_OUT_ITEM> IterableOutputBuilder<NEXT_OUT_ITEM> then(final IterableOutput<NEXT_OUT_ITEM> op) {
            ops.add(op);
            return new IterableOutputBuilder<>(ops);
        }

        public OperationChain<Void> build() {
            return new OperationChain<>(ops);
        }
    }

    public static final class OutputBuilder<OUT> {
        private final List<Operation> ops;

        private OutputBuilder(final Output<OUT> op) {
            this(new ArrayList<>());
            ops.add(op);
        }

        private OutputBuilder(final List<Operation> ops) {
            this.ops = ops;
        }

        public OutputBuilder<OUT> then(final InputOutputT<OUT> op) {
            ops.add(op);
            return new OutputBuilder<>(ops);
        }

        public NoOutputBuilder then(final Input<? super OUT> op) {
            ops.add(op);
            return new NoOutputBuilder(ops);
        }

        public <NEXT_OUT> OutputBuilder<NEXT_OUT> then(final InputOutput<? super OUT, NEXT_OUT> op) {
            ops.add(op);
            return new OutputBuilder<>(ops);
        }

        public <NEXT_OUT_ITEM> IterableOutputBuilder<NEXT_OUT_ITEM> then(final InputIterableOutput<? super OUT, NEXT_OUT_ITEM> op) {
            ops.add(op);
            return new IterableOutputBuilder<>(ops);
        }

        public OperationChain<OUT> build() {
            return new OperationChain<>(ops);
        }
    }

    public static final class IterableOutputBuilder<OUT_ITEM> {
        private final List<Operation> ops;

        private IterableOutputBuilder(final IterableOutput<OUT_ITEM> op) {
            this(new ArrayList<>());
            ops.add(op);
        }

        private IterableOutputBuilder(final List<Operation> ops) {
            this.ops = ops;
        }

        public IterableOutputBuilder<OUT_ITEM> then(final InputOutputT<Iterable<? super OUT_ITEM>> op) {
            ops.add(op);
            return new IterableOutputBuilder<>(ops);
        }

        public NoOutputBuilder then(final Input<? super Iterable<? super OUT_ITEM>> op) {
            ops.add(op);
            return new NoOutputBuilder(ops);
        }

        public <NEXT_OUT> OutputBuilder<NEXT_OUT> then(final InputOutput<? super Iterable<? super OUT_ITEM>, NEXT_OUT> op) {
            ops.add(op);
            return new OutputBuilder<>(ops);
        }

        public <NEXT_OUT> IterableOutputBuilder<NEXT_OUT> then(final InputIterableOutput<? super Iterable<? super OUT_ITEM>, NEXT_OUT> op) {
            ops.add(op);
            return new IterableOutputBuilder<>(ops);
        }

        public NoOutputBuilder then(final IterableInput<? super OUT_ITEM> op) {
            ops.add(op);
            return new NoOutputBuilder(ops);
        }

        public <NEXT_OUT> OutputBuilder<NEXT_OUT> then(final IterableInputOutput<? super OUT_ITEM, NEXT_OUT> op) {
            ops.add(op);
            return new OutputBuilder<>(ops);
        }

        public <NEXT_OUT> IterableOutputBuilder<NEXT_OUT> then(final IterableInputIterableOutput<? super OUT_ITEM, NEXT_OUT> op) {
            ops.add(op);
            return new IterableOutputBuilder<>(ops);
        }

        public OperationChain<CloseableIterable<OUT_ITEM>> build() {
            return new OperationChain<>(ops);
        }
    }
}
