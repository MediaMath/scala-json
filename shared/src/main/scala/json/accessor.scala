/*
 * Copyright 2016 MediaMath, Inc
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

package json

import json.internal.ObjectAccessorFactory

import scala.annotation.StaticAnnotation

//this has to be remote because compiler complains if the macro needs this type
@json.internal.CompileTimeOnly.anno("enable macro paradise to expand macro annotations. https://github.com/MediaMath/scala-json/blob/master/README.md#dependencies")
/** Automatically provides an implicit ObjectAccessor to a class companion object */
class accessor extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ObjectAccessorFactory.annotation_impl
}

