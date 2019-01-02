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

import de.interactive_instruments.IFile;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.dal.dao.DataStorage;
import de.interactive_instruments.etf.dal.dto.result.*;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.dal.dto.test.*;
import de.interactive_instruments.etf.dal.dto.translation.TranslationArgumentCollectionDto;
import de.interactive_instruments.etf.model.DefaultEidMap;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.model.EidMap;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.etf.testdriver.TestRunLogger;
import de.interactive_instruments.etf.testdriver.TestTaskEndListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class InMemoryTestResultCollector implements TestResultCollector {

	private final DataStorage inMemoryStorage;
	private final TestRunLogger logger;
	private final TestTaskDto testTaskDto;
	private final EidMap<AttachmentDto> attachments = new DefaultEidMap<>();

	private final TestTaskResultDto testTaskResult = new TestTaskResultDto();
	private final List<TestModuleResultDto> testModuleResults = new ArrayList<>();
	private final List<TestCaseResultDto> testCaseResults = new ArrayList<>();
	private final List<TestStepResultDto> testStepResults = new ArrayList<>();
	private final List<TestAssertionResultDto> testAssertionResults = new ArrayList<>();

	private final static String resultID = "00000000-0000-0000-C000-000000000046";
	private TestTaskEndListener listener;
	private int currentModelType;

	public InMemoryTestResultCollector(final DataStorage inMemoryStorage, final TestRunLogger logger, final TestTaskDto testTaskDto) {
		this.inMemoryStorage = inMemoryStorage;
		this.logger = logger;
		this.testTaskDto = testTaskDto;
		currentModelType = 0;
	}

	@Override public IFile getAttachmentDir() {
		return null;
	}

	@Override public IFile getResultFile() {
		return null;
	}

	@Override public String getTestTaskResultId() {
		return resultID;
	}

	@Override public boolean endWithSkippedIfTestCasesFailed(final String... testCaseIds) throws IllegalArgumentException, IllegalStateException {
		return false;
	}

	@Override public TestResultStatus status(final String testModelItemId) throws IllegalArgumentException {
		return null;
	}

	@Override public boolean statusEqualsAny(final String testModelItemId, final String... testResultStatus) throws IllegalArgumentException {
		return false;
	}

	@Override public boolean isErrorLimitExceeded() {
		return false;
	}

	private EID createAttachment(String label, String encoding, String mimeType, String type) {
		final AttachmentDto attachmentDto = new AttachmentDto();
		attachmentDto.setLabel(label);
		attachmentDto.setEncoding(encoding);
		attachmentDto.setMimeType(mimeType);
		attachmentDto.setType(type);
		attachmentDto.setEmbeddedData("dummy");
		attachmentDto.setId(EidFactory.getDefault().createRandomId());
		this.attachments.put(attachmentDto.getId(), attachmentDto);
		return attachmentDto.getId();
	}

	@Override public String markAttachment(final String fileName, final String label, final String encoding, final String mimeType, final String type) throws IOException {
		return createAttachment(label, encoding, mimeType, type).getId();
	}

	@Override public String saveAttachment(final Reader reader, final String label, final String mimeType, final String type) throws IOException {
		return createAttachment(label, "UTF-8", mimeType, type).getId();
	}

	@Override public String saveAttachment(final InputStream inputStream, final String label, final String mimeType, final String type) throws IOException {
		return createAttachment(label, "UTF-8", mimeType, type).getId();
	}

	@Override public String saveAttachment(final String content, final String label, final String mimeType, final String type) throws IOException {
		return createAttachment(label, "UTF-8", mimeType, type).getId();
	}

	@Override public File getTempDir() {
		return null;
	}

	@Override public void internalError(final String translationTemplateId, final Map<String, String> tokenValuePairs, final Throwable e) {

	}

	@Override public void internalError(final Throwable e) {
		this.testTaskResult.setInternalError((Exception) e);
	}

	@Override public String internalError(final String errorMessage, final byte[] bytes, final String mimeType) {
		return null;
	}

	@Override public TestRunLogger getLogger() {
		return this.logger;
	}

	private void stepDeeper(final int levelCheck) {
		assert currentModelType>-1;
		assert currentModelType<6;
		assert currentModelType<levelCheck;
		currentModelType=levelCheck;
	}

	@Override public String startTestTask(final String testTaskId, final long startTimestamp) throws IllegalArgumentException, IllegalStateException {
		stepDeeper(1);
		this.testTaskResult.setId(EidFactory.getDefault().createRandomId());
		this.testTaskResult.setResultedFrom(this.testTaskDto.getExecutableTestSuite());
		this.testTaskResult.setStartTimestamp(new Date(startTimestamp));
		return testTaskResult.getId().getId();
	}

	@Override public String startTestModule(final String testModuleId, final long startTimestamp) throws IllegalArgumentException, IllegalStateException {
		stepDeeper(2);
		final TestModuleResultDto testModuleResultDto = new TestModuleResultDto();
		testModuleResultDto.setId(EidFactory.getDefault().createRandomId());
		this.testTaskResult.setResultedFrom(this.testTaskDto.getExecutableTestSuite().getChildrenAsMap().get(testModuleId));
		this.testModuleResults.add(testModuleResultDto);
		testModuleResultDto.setStartTimestamp(new Date(startTimestamp));
		return testModuleResultDto.getId().getId();
	}

	@Override public String startTestCase(final String testCaseId, final long startTimestamp) throws IllegalArgumentException, IllegalStateException {
		stepDeeper(3);
		final TestCaseResultDto testCaseResultDto = new TestCaseResultDto();
		testCaseResultDto.setId(EidFactory.getDefault().createRandomId());
		for (final TestModuleDto testModule : this.testTaskDto.getExecutableTestSuite().getTestModules()) {
			final TestModelItemDto testCase = testModule.getChildrenAsMap().get(testCaseId);
			if(testCase!=null) {
				testCaseResultDto.setResultedFrom(testCase);
				break;
			}
		}
		testCaseResultDto.setStartTimestamp(new Date(startTimestamp));
		this.testCaseResults.add(testCaseResultDto);
		return testCaseResultDto.getId().getId();
	}

	@Override public String startTestStep(final String testStepId, final long startTimestamp) throws IllegalArgumentException, IllegalStateException {
		stepDeeper(4);
		final TestStepResultDto testStepResultDto = new TestStepResultDto();
		testStepResultDto.setId(EidFactory.getDefault().createRandomId());
		out:
		for (final TestModuleDto testModule : this.testTaskDto.getExecutableTestSuite().getTestModules()) {
			for (final TestCaseDto testCase : testModule.getTestCases()) {
				final TestModelItemDto testStep = testCase.getChildrenAsMap().get(testStepId);
				if(testStep!=null) {
					testStepResultDto.setResultedFrom(testStep);
					break out;
				}
			}
		}
		testStepResultDto.setStartTimestamp(new Date(startTimestamp));
		this.testStepResults.add(testStepResultDto);
		return testStepResultDto.getId().getId();
	}

	@Override public String startTestAssertion(final String testAssertionId, final long startTimestamp) throws IllegalArgumentException, IllegalStateException {
		stepDeeper(5);
		final TestAssertionResultDto testAssertionResultDto = new TestAssertionResultDto();
		testAssertionResultDto.setId(EidFactory.getDefault().createRandomId());
		out:
		for (final TestModuleDto testModule : this.testTaskDto.getExecutableTestSuite().getTestModules()) {
			for (final TestCaseDto testCase : testModule.getTestCases()) {
				for (final TestStepDto testStep : testCase.getTestSteps()) {
					final TestModelItemDto assertion = testStep.getChildrenAsMap().get(testAssertionId);
					if(assertion!=null) {
						testAssertionResultDto.setResultedFrom(assertion);
						break out;
					}
				}
			}
		}
		testAssertionResultDto.setStartTimestamp(new Date(startTimestamp));
		this.testAssertionResults.add(testAssertionResultDto);
		return testAssertionResultDto.getId().getId();
	}

	private void setResult(final String testModelItemId, final ResultModelItemDto item, final long stopTimestamp, final int status) {
		if(!item.getId().equals(testModelItemId)) {
			currentModelType++;
			throw new IllegalArgumentException("Invalid ID: "+testModelItemId);
		}
		item.setDuration(stopTimestamp-item.getStartTimestamp().getTime());
		final List<? extends ResultModelItemDto> children = item.getChildren();
		if(children!=null) {
			final List<TestResultStatus> resultStatus = children.stream().map(
					ResultModelItemDto::getResultStatus).collect(Collectors.toList());
			resultStatus.add(TestResultStatus.valueOf(status));
			item.setResultStatus(TestResultStatus.aggregateStatus(resultStatus));
		}else{
			item.setResultStatus(TestResultStatus.valueOf(status));
		}
	}

	@Override public String end(final String testModelItemId, final int status, final long stopTimestamp) throws IllegalArgumentException, IllegalStateException {
		switch (currentModelType--) {
		case 1:
			this.listener.testTaskFinished(this.testTaskResult);
			setResult(testModelItemId, this.testTaskResult, stopTimestamp, status);
			return testTaskResult.getId().getId();
		case 2:
			final TestModuleResultDto moduleResult = this.testModuleResults.get(this.testModuleResults.size() - 1);
			setResult(testModelItemId, moduleResult, stopTimestamp, status);
			return moduleResult.getId().getId();
		case 3:
			final TestCaseResultDto testCaseResultDto = this.testCaseResults.get(this.testCaseResults.size() - 1);
			setResult(testModelItemId, testCaseResultDto, stopTimestamp, status);
			return testCaseResultDto.getId().getId();
		case 4:
			final TestStepResultDto testStepDto = this.testStepResults.get(this.testStepResults.size() - 1);
			setResult(testModelItemId, testStepDto, stopTimestamp, status);
			return testStepDto.getId().getId();
		case 5:
			final TestAssertionResultDto testAssertionDto = this.testAssertionResults.get(this.testAssertionResults.size() - 1);
			setResult(testModelItemId, testAssertionDto, stopTimestamp, status);
			return testAssertionDto.getId().getId();
		default:
			currentModelType++;
			throw new IllegalStateException("Illegal state: "+currentModelType);
		}
	}

	@Override public String end(final String testModelItemId, final long stopTimestamp) throws IllegalArgumentException, IllegalStateException {
		return end(testModelItemId, 0, stopTimestamp);
	}

	@Override public void addMessage(final String translationTemplateId) {
		addMessage(translationTemplateId, (Map<String, String>) null);
	}

	@Override public void addMessage(final String translationTemplateId, final Map<String, String> tokenValuePairs) {
		final TestAssertionResultDto testAssertionDto = this.testAssertionResults.get(this.testAssertionResults.size() - 1);
		final TranslationArgumentCollectionDto transArgument = new TranslationArgumentCollectionDto();
		transArgument.setRefTemplateName(translationTemplateId);
		if(tokenValuePairs!=null) {
			for (final Map.Entry<String, String> e : tokenValuePairs.entrySet()) {
				transArgument.addTokenValue(e.getKey(),e.getValue());
			}
		}
		testAssertionDto.addMessage(transArgument);
	}

	@Override public void addMessage(final String translationTemplateId, final String... tokensAndValues) {
		addMessage(translationTemplateId, SUtils.toStrMap(tokensAndValues));
	}

	@Override public int currentModelType() {
		return this.currentModelType;
	}

	@Override public void registerTestTaskEndListener(final TestTaskEndListener listener) {
		this.listener=listener;
	}

	@Override public void release() {

	}
}
