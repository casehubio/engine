/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.annotations.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.engine.annotations.Case;
import io.casehub.engine.annotations.Cost;
import io.casehub.engine.annotations.PlanningMode;
import io.casehub.engine.annotations.Worker;
import io.casehub.engine.plan.goap.GoapWorldState;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CostValidationTest {

  @RegisterExtension
  static final QuarkusUnitTest wrongSignature =
      new QuarkusUnitTest()
          .withApplicationRoot(root -> root.addClasses(WrongSignatureCase.class))
          .assertException(t -> assertThat(t.getMessage()).contains("GoapWorldState"));

  @Case(namespace = "test", name = "WrongSig", planning = PlanningMode.GOAP)
  public interface WrongSignatureCase {

    @Worker(capability = "work")
    default String doWork(String input) {
      return input;
    }

    @Cost("work")
    default double workCost(String notWorldState) {
      return 1.0;
    }
  }

  @Test
  void wrong_signature_rejected() {}

  @RegisterExtension
  static final QuarkusUnitTest wrongReturnType =
      new QuarkusUnitTest()
          .withApplicationRoot(root -> root.addClasses(WrongReturnCase.class))
          .assertException(t -> assertThat(t.getMessage()).contains("return double"));

  @Case(namespace = "test", name = "WrongReturn", planning = PlanningMode.GOAP)
  public interface WrongReturnCase {

    @Worker(capability = "work")
    default String doWork(String input) {
      return input;
    }

    @Cost("work")
    default String workCost(GoapWorldState state) {
      return "not a double";
    }
  }

  @Test
  void wrong_return_type_rejected() {}

  @RegisterExtension
  static final QuarkusUnitTest unknownWorker =
      new QuarkusUnitTest()
          .withApplicationRoot(root -> root.addClasses(UnknownWorkerCase.class))
          .assertException(t -> assertThat(t.getMessage()).contains("unknown worker"));

  @Case(namespace = "test", name = "UnknownWorker", planning = PlanningMode.GOAP)
  public interface UnknownWorkerCase {

    @Worker(capability = "work")
    default String doWork(String input) {
      return input;
    }

    @Cost("nonExistent")
    default double badCost(GoapWorldState state) {
      return 1.0;
    }
  }

  @Test
  void unknown_worker_reference_rejected() {}

  @RegisterExtension
  static final QuarkusUnitTest costOnWorker =
      new QuarkusUnitTest()
          .withApplicationRoot(root -> root.addClasses(CostOnWorkerCase.class))
          .assertException(
              t -> assertThat(t.getMessage()).contains("cannot be on the same method"));

  @Case(namespace = "test", name = "CostOnWorker", planning = PlanningMode.GOAP)
  public interface CostOnWorkerCase {

    @Worker(capability = "work")
    @Cost("work")
    default double doWork(GoapWorldState state) {
      return 1.0;
    }
  }

  @Test
  void cost_on_worker_method_rejected() {}
}
