/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.test.api.runtime.migration;

import static org.camunda.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.camunda.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.camunda.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;

import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.junit.Test;

public class MigrationRemoveBoundaryEventsTest extends AbstractMigrationTest {

  @Test
  public void testRemoveMessageBoundaryEventFromUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.ONE_TASK_PROCESS)
      .addMessageBoundaryEventWithUserTask("userTask", "boundary", MESSAGE_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());


    assertEventSubscriptionRemoved("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveMessageBoundaryEventFromScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SCOPE_TASK_PROCESS)
      .addMessageBoundaryEventWithUserTask("userTask", "boundary", MESSAGE_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SCOPE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveMessageBoundaryEventFromConcurrentUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .addMessageBoundaryEventWithUserTask("userTask1", "boundary", MESSAGE_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.PARALLEL_GATEWAY_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask1").concurrent().noScope()
          .up()
          .child("userTask2").concurrent().noScope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask1", "userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveMessageBoundaryEventFromConcurrentScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.PARALLEL_SCOPE_TASKS)
      .addMessageBoundaryEventWithUserTask("userTask1", "boundary", MESSAGE_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.PARALLEL_SCOPE_TASKS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).concurrent().noScope()
          .child("userTask1").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask1"))
          .up().up()
          .child(null).concurrent().noScope()
          .child("userTask2").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask2"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask1", "userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveMessageBoundaryEventFromSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SUBPROCESS_PROCESS)
      .addMessageBoundaryEventWithUserTask("subProcess", "boundary", MESSAGE_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveMessageBoundaryEventFromSubProcessWithScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS)
      .addMessageBoundaryEventWithUserTask("subProcess", "boundary", MESSAGE_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveMessageBoundaryEventFromParallelSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.PARALLEL_SUBPROCESS_PROCESS)
      .addMessageBoundaryEventWithUserTask("subProcess1", "boundary", MESSAGE_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess1")
      .mapActivities("subProcess2", "subProcess2")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).concurrent().noScope()
          .child("userTask1").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess1"))
          .up().up()
          .child(null).concurrent().noScope()
          .child("userTask2").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess2"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess1", testHelper.getSingleActivityInstanceBeforeMigration("subProcess1").getId())
          .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .endScope()
        .beginScope("subProcess2", testHelper.getSingleActivityInstanceBeforeMigration("subProcess2").getId())
          .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask1", "userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveMessageBoundaryEventFromUserTaskInSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SUBPROCESS_PROCESS)
      .addMessageBoundaryEventWithUserTask("userTask", "boundary", MESSAGE_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", MESSAGE_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveSignalBoundaryEventFromUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.ONE_TASK_PROCESS)
      .addSignalBoundaryEventWithUserTask("userTask", "boundary", SIGNAL_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveSignalBoundaryEventFromScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SCOPE_TASK_PROCESS)
      .addSignalBoundaryEventWithUserTask("userTask", "boundary", SIGNAL_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SCOPE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveSignalBoundaryEventFromConcurrentUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .addSignalBoundaryEventWithUserTask("userTask1", "boundary", SIGNAL_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.PARALLEL_GATEWAY_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask1").concurrent().noScope()
          .up()
          .child("userTask2").concurrent().noScope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask1", "userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveSignalBoundaryEventFromConcurrentScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.PARALLEL_SCOPE_TASKS)
      .addSignalBoundaryEventWithUserTask("userTask1", "boundary", SIGNAL_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.PARALLEL_SCOPE_TASKS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).concurrent().noScope()
          .child("userTask1").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask1"))
          .up().up()
          .child(null).concurrent().noScope()
          .child("userTask2").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask2"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask1", "userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveSignalBoundaryEventFromSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SUBPROCESS_PROCESS)
      .addSignalBoundaryEventWithUserTask("subProcess", "boundary", SIGNAL_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveSignalBoundaryEventFromSubProcessWithScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS)
      .addSignalBoundaryEventWithUserTask("subProcess", "boundary", SIGNAL_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveSignalBoundaryEventFromParallelSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.PARALLEL_SUBPROCESS_PROCESS)
      .addSignalBoundaryEventWithUserTask("subProcess1", "boundary", SIGNAL_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess1")
      .mapActivities("subProcess2", "subProcess2")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).concurrent().noScope()
          .child("userTask1").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess1"))
          .up().up()
          .child(null).concurrent().noScope()
          .child("userTask2").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess2"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess1", testHelper.getSingleActivityInstanceBeforeMigration("subProcess1").getId())
          .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .endScope()
        .beginScope("subProcess2", testHelper.getSingleActivityInstanceBeforeMigration("subProcess2").getId())
          .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask1", "userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveSignalBoundaryEventFromUserTaskInSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SUBPROCESS_PROCESS)
      .addSignalBoundaryEventWithUserTask("userTask", "boundary", SIGNAL_NAME, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess")
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("boundary", SIGNAL_NAME);

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveTimerBoundaryEventFromUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.ONE_TASK_PROCESS)
      .addTimerDateBoundaryEventWithUserTask("userTask", "boundary", TIMER_DATE, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.ONE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertTimerJobRemoved("boundary");

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveTimerBoundaryEventFromScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SCOPE_TASK_PROCESS)
      .addTimerDateBoundaryEventWithUserTask("userTask", "boundary", TIMER_DATE, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SCOPE_TASK_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertTimerJobRemoved("boundary");

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveTimerBoundaryEventFromConcurrentUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.PARALLEL_GATEWAY_PROCESS)
      .addTimerDateBoundaryEventWithUserTask("userTask1", "boundary", TIMER_DATE, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.PARALLEL_GATEWAY_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask1").concurrent().noScope()
          .up()
          .child("userTask2").concurrent().noScope()
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    assertTimerJobRemoved("boundary");

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask1", "userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveTimerBoundaryEventFromConcurrentScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.PARALLEL_SCOPE_TASKS)
      .addTimerDateBoundaryEventWithUserTask("userTask1", "boundary", TIMER_DATE, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.PARALLEL_SCOPE_TASKS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).concurrent().noScope()
          .child("userTask1").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask1"))
          .up().up()
          .child(null).concurrent().noScope()
          .child("userTask2").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask2"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    assertTimerJobRemoved("boundary");

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask1", "userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveTimerBoundaryEventFromSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SUBPROCESS_PROCESS)
      .addTimerDateBoundaryEventWithUserTask("subProcess", "boundary", TIMER_DATE, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertTimerJobRemoved("boundary");

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveTimerBoundaryEventFromSubProcessWithScopeUserTask() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS)
      .addTimerDateBoundaryEventWithUserTask("subProcess", "boundary", TIMER_DATE, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SCOPE_TASK_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("userTask"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertTimerJobRemoved("boundary");

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveTimerBoundaryEventFromParallelSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.PARALLEL_SUBPROCESS_PROCESS)
      .addTimerDateBoundaryEventWithUserTask("subProcess1", "boundary", TIMER_DATE, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.PARALLEL_SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess1", "subProcess1")
      .mapActivities("subProcess2", "subProcess2")
      .mapActivities("userTask1", "userTask1")
      .mapActivities("userTask2", "userTask2")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).concurrent().noScope()
          .child("userTask1").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess1"))
          .up().up()
          .child(null).concurrent().noScope()
          .child("userTask2").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess2"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess1", testHelper.getSingleActivityInstanceBeforeMigration("subProcess1").getId())
          .activity("userTask1", testHelper.getSingleActivityInstanceBeforeMigration("userTask1").getId())
        .endScope()
        .beginScope("subProcess2", testHelper.getSingleActivityInstanceBeforeMigration("subProcess2").getId())
          .activity("userTask2", testHelper.getSingleActivityInstanceBeforeMigration("userTask2").getId())
        .done());

    assertTimerJobRemoved("boundary");

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask1", "userTask2");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveTimerBoundaryEventFromUserTaskInSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SUBPROCESS_PROCESS)
      .addTimerDateBoundaryEventWithUserTask("userTask", "boundary", TIMER_DATE, AFTER_BOUNDARY_TASK)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertTimerJobRemoved("boundary");

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveMultipleBoundaryEvents() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SUBPROCESS_PROCESS)
      .addTimerDateBoundaryEvent("subProcess", "timerBoundary", TIMER_DATE)
      .addMessageBoundaryEvent("userTask", "messageBoundary", MESSAGE_NAME)
      .addSignalBoundaryEvent("userTask", "signalBoundary", SIGNAL_NAME)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
          .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    assertEventSubscriptionRemoved("messageBoundary", MESSAGE_NAME);
    assertEventSubscriptionRemoved("signalBoundary", SIGNAL_NAME);
    assertTimerJobRemoved("timerBoundary");

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveErrorBoundaryEventFromSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SUBPROCESS_PROCESS)
      .addErrorBoundaryEvent("subProcess", ERROR_CODE)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
        .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

  @Test
  public void testRemoveEscalationBoundaryEventFromSubProcess() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deploy(modify(ProcessModels.SUBPROCESS_PROCESS)
      .addEscalationBoundaryEvent("subProcess", ESCALATION_CODE)
    );
    ProcessDefinition targetProcessDefinition = testHelper.deploy(ProcessModels.SUBPROCESS_PROCESS);

    MigrationPlan migrationPlan = runtimeService
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("subProcess", "subProcess")
      .mapActivities("userTask", "userTask")
      .build();

    // when
    testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("userTask").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("subProcess"))
          .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess", testHelper.getSingleActivityInstanceBeforeMigration("subProcess").getId())
        .activity("userTask", testHelper.getSingleActivityInstanceBeforeMigration("userTask").getId())
        .done());

    // and it is possible to successfully complete the migrated instance
    completeTasks("userTask");
    testHelper.assertProcessEnded(testHelper.snapshotBeforeMigration.getProcessInstanceId());
  }

}
