/*
 * Copyright 2010-2019 interactive instruments GmbH
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

package de.interactive_instruments.etf.test;

import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.etf.testdriver.TestResultCollectorFactory;
import de.interactive_instruments.etf.testdriver.TestRunLogger;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class InMemoryResultCollectorFactory implements TestResultCollectorFactory {

	@Override public TestResultCollector createTestResultCollector(final TestRunLogger logger, final TestTaskDto testTaskDto) {
		return new InMemoryTestResultCollector(DataStorageTestUtils.inMemoryStorage(), logger, testTaskDto);
	}
}
