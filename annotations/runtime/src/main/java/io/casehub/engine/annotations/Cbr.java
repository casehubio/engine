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
package io.casehub.engine.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cbr {
  Feature[] features();

  Weight[] weights() default {};

  int topK() default 5;

  double minSimilarity() default 0;

  String domain() default "";

  String caseType() default "";

  double vectorWeight() default 0;

  String timing() default "per-evaluation";

  String cbrType() default "";

  int temporalDecayHalfLifeDays() default -1;

  int minCostSamples() default -1;

  boolean crossType() default false;

  String problemDescription() default "";
}
