<template>
  <el-form
    ref="studentForm"
    :model="studentBeingEdited"
    :rules="rules"
    label-width="110px"
    style="padding-right: 30px"
    label-position="left"
  >
    <el-form-item label="Id:">
      <el-input v-model="studentBeingEdited.id" disabled></el-input>
    </el-form-item>
    <el-form-item label="Username:" prop="username">
      <el-input v-model="studentBeingEdited.username"></el-input>
    </el-form-item>
    <el-form-item label="Status:">
      <!-- UC-STU-deactivate-student: deactivation is its own confirmed action, not a field of this form.
           3a: a deactivated student shows her status and no Deactivate button. -->
      <el-tag v-if="studentBeingEdited.enabled" type="success">Active</el-tag>
      <el-tag v-else type="danger">Deactivated</el-tag>
      <el-button
        v-if="studentBeingEdited.enabled"
        type="danger"
        plain
        size="small"
        style="margin-left: 12px"
        :loading="deactivating"
        @click="confirmAndDeactivate()"
      >
        Deactivate
      </el-button>
    </el-form-item>
    <el-form-item label="First Name:" prop="firstName">
      <el-input v-model="studentBeingEdited.firstName"></el-input>
    </el-form-item>
    <el-form-item label="Last Name:" prop="lastName">
      <el-input v-model="studentBeingEdited.lastName"></el-input>
    </el-form-item>
    <el-form-item label="Email:" prop="email">
      <el-input v-model="studentBeingEdited.email"></el-input>
    </el-form-item>
    <el-form-item label="Team:" prop="teamName">
      <el-input v-model="studentBeingEdited.teamName" disabled></el-input>
    </el-form-item>
    <div style="display: flex; justify-content: end">
      <el-button @click="$emit('close-dialog')"> Cancel </el-button>
      <el-button type="primary" @click="updateExistingStudent()"> Confirm </el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Student } from '@/apis/student/types'
import type { FormInstance } from 'element-plus'
import { deactivateStudent, findStudentById, updateStudent } from '@/apis/student'
import { ref, onMounted } from 'vue'

const { studentId } = defineProps(['studentId'])
const emit = defineEmits(['close-dialog', 'student-deactivated'])

// Note that the structure of the userData is NOT the same as the User interface since the roles are stored as an array
const studentBeingEdited = ref<Student>({
  // Data of the user being added or edited
  id: NaN,
  username: '',
  enabled: false,
  firstName: '',
  lastName: '',
  email: ''
})

const studentForm = ref<FormInstance>() // Form reference

const deactivating = ref(false) // Disables the button while the request is in flight

// Validation rules
const rules = {
  username: [
    { required: true, message: 'Please input your email as your username', trigger: 'blur' },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: 'Enter your email as username.',
      trigger: 'blur'
    }
  ],
  firstName: [
    { required: true, message: 'Please provide the first name of the user.', trigger: 'blur' }
  ],
  lastName: [
    { required: true, message: 'Please provide the last name of the user.', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please provide the email of the user.', trigger: 'blur' },
    { type: 'email', message: 'Please provide a valid email.', trigger: 'blur' }
  ]
}

async function loadStudent(studentId: number) {
  // Load the user to be edited
  const result = await findStudentById(studentId)

  studentBeingEdited.value.id = result.data.id as number
  studentBeingEdited.value.username = result.data.username
  studentBeingEdited.value.enabled = result.data.enabled
  studentBeingEdited.value.firstName = result.data.firstName
  studentBeingEdited.value.lastName = result.data.lastName
  studentBeingEdited.value.email = result.data.email
  studentBeingEdited.value.teamName = result.data.teamName
}

onMounted(() => {
  loadStudent(studentId) // Load the artifact to be edited
})

async function updateExistingStudent() {
  await studentForm.value!.validate() // Validate the form

  const updatedStudent: Student = {
    id: studentBeingEdited.value.id,
    username: studentBeingEdited.value.username,
    enabled: studentBeingEdited.value.enabled,
    firstName: studentBeingEdited.value.firstName,
    lastName: studentBeingEdited.value.lastName,
    email: studentBeingEdited.value.email
  }
  // Call the API to update the user
  await updateStudent(updatedStudent)

  ElMessage.success('Student updated successfully')
  emit('close-dialog')
}

async function confirmAndDeactivate() {
  const fullName = `${studentBeingEdited.value.firstName} ${studentBeingEdited.value.lastName}`
  // Step 4: warn and name the consequences (UC-STU-deactivate-student Associated Information)
  try {
    await ElMessageBox.confirm(
      `${fullName} will no longer be able to log in, and is logged out at her next action if she is ` +
        `logged in now. She will not be reminded or peer-evaluated. She stays on her team, and her weekly ` +
        `activity reports and peer evaluations are kept. Reactivation is not yet available on this screen.`,
      `Deactivate ${fullName}?`,
      {
        confirmButtonText: 'Deactivate',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }
    )
  } catch {
    return // 5a: cancelled, nothing is sent
  }

  deactivating.value = true
  try {
    // Step 6. A 403 (3b) or 409 (6a) is shown by the request interceptor's error message.
    const result = await deactivateStudent(studentBeingEdited.value.id as number)
    studentBeingEdited.value.enabled = result.data.enabled
    ElMessage.success(`${fullName} has been deactivated.`)
    emit('student-deactivated')
  } catch {
    await loadStudent(studentId) // 6a: show the current status
  } finally {
    deactivating.value = false
  }
}
</script>

<style scoped lang="scss"></style>
