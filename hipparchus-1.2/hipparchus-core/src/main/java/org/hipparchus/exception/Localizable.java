/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This is not the original file distributed by the Apache Software Foundation
 * It has been modified by the Hipparchus project
 */
package org.hipparchus.exception;

import java.io.Serializable;
import java.util.Locale;

/**
 * Interface for localizable strings.
 *
 */
public interface Localizable extends Serializable {
    /**
     * Gets the source (non-localized) string.
     *
     * @return the source string.
     */
    String getSourceString();

    /**
     * Gets the localized string.
     *
     * @param locale locale into which to get the string.
     * @return the localized string or the source string if no
     * localized version is available.
     */
    String getLocalizedString(Locale locale);
}
