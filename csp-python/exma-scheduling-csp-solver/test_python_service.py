import requests
import json
from datetime import date, time
from typing import Dict, Any

# Base URL for the Python service
BASE_URL = "http://localhost:8009"

def create_simple_test_case() -> Dict[str, Any]:
    """Simple case: 3 courses, 3 rooms, minimal constraints"""
    return {
        "examPeriod": {
            "examSessionPeriodId": "SUMMER_2025_MIDTERM",
            "academicYear": "2024-2025",
            "examSession": "Midterm",
            "startDate": "2025-06-15",
            "endDate": "2025-06-20"
        },
        "courses": [
            {
                "courseId": "CS101",
                "courseName": "Introduction to Computer Science",
                "studentCount": 50,
                "professorIds": ["PROF001"],
                "mandatoryStatus": "MANDATORY",
                "estimatedDuration": 120,
                "requiredEquipment": [],
                "accessibilityRequired": False
            },
            {
                "courseId": "MATH201",
                "courseName": "Calculus II",
                "studentCount": 30,
                "professorIds": ["PROF002"],
                "mandatoryStatus": "MANDATORY",
                "estimatedDuration": 120,
                "requiredEquipment": [],
                "accessibilityRequired": False
            },
            {
                "courseId": "ENG301",
                "courseName": "Technical Writing",
                "studentCount": 25,
                "professorIds": ["PROF003"],
                "mandatoryStatus": "ELECTIVE",
                "estimatedDuration": 90,
                "requiredEquipment": [],
                "accessibilityRequired": False
            }
        ],
        "availableRooms": [
            {
                "roomId": "ROOM_A101",
                "roomName": "Amphitheater A101",
                "capacity": 80,
                "equipment": ["projector", "microphone"],
                "location": "Building A",
                "accessibility": True
            },
            {
                "roomId": "ROOM_B205",
                "roomName": "Classroom B205",
                "capacity": 40,
                "equipment": ["whiteboard"],
                "location": "Building B",
                "accessibility": True
            },
            {
                "roomId": "ROOM_C302",
                "roomName": "Lab C302",
                "capacity": 30,
                "equipment": ["computers", "projector"],
                "location": "Building C",
                "accessibility": True
            }
        ],
        "professorPreferences": [
            {
                "preferenceId": "PREF001",
                "professorId": "PROF001",
                "courseId": "CS101",
                "preferredDates": ["2025-06-16", "2025-06-17"],
                "preferredTimeSlots": [
                    {"startTime": "09:00:00", "endTime": "11:00:00"},
                    {"startTime": "10:00:00", "endTime": "12:00:00"}
                ],
                "unavailableDates": [],
                "unavailableTimeSlots": [],
                "preferredRooms": ["ROOM_A101"],
                "specialRequirements": "Prefers morning slots",
                "priority": 1
            },
            {
                "preferenceId": "PREF002",
                "professorId": "PROF002",
                "courseId": "MATH201",
                "preferredDates": ["2025-06-18"],
                "preferredTimeSlots": [
                    {"startTime": "14:00:00", "endTime": "16:00:00"}
                ],
                "unavailableDates": [],
                "unavailableTimeSlots": [],
                "preferredRooms": [],
                "specialRequirements": "Afternoon preference",
                "priority": 1
            }
        ],
        "institutionalConstraints": {
            "workingHours": {
                "startTime": "08:00:00",
                "endTime": "18:00:00"
            },
            "minimumExamDuration": 90,
            "minimumGapMinutes": 30,
            "maxExamsPerDay": 6,
            "maxExamsPerRoom": 8,
            "allowWeekendExams": False
        }
    }

def create_complex_test_case() -> Dict[str, Any]:
    """Complex case: Many courses, limited rooms, conflicts"""
    return {
        "examPeriod": {
            "examSessionPeriodId": "SUMMER_2025_FINALS",
            "academicYear": "2024-2025",
            "examSession": "Finals",
            "startDate": "2025-06-20",
            "endDate": "2025-06-25"
        },
        "courses": [
            {
                "courseId": "CS101",
                "courseName": "Introduction to Computer Science",
                "studentCount": 120,
                "professorIds": ["PROF001", "PROF002"],
                "mandatoryStatus": "MANDATORY",
                "estimatedDuration": 180,
                "requiredEquipment": ["computers"],
                "accessibilityRequired": True
            },
            {
                "courseId": "CS201",
                "courseName": "Data Structures",
                "studentCount": 80,
                "professorIds": ["PROF001"],
                "mandatoryStatus": "MANDATORY",
                "estimatedDuration": 150,
                "requiredEquipment": ["computers"],
                "accessibilityRequired": False
            },
            {
                "courseId": "MATH101",
                "courseName": "Linear Algebra",
                "studentCount": 90,
                "professorIds": ["PROF003"],
                "mandatoryStatus": "MANDATORY",
                "estimatedDuration": 120,
                "requiredEquipment": [],
                "accessibilityRequired": False
            },
            {
                "courseId": "MATH201",
                "courseName": "Calculus II",
                "studentCount": 60,
                "professorIds": ["PROF004"],
                "mandatoryStatus": "MANDATORY",
                "estimatedDuration": 120,
                "requiredEquipment": [],
                "accessibilityRequired": False
            },
            {
                "courseId": "ENG301",
                "courseName": "Technical Writing",
                "studentCount": 35,
                "professorIds": ["PROF005"],
                "mandatoryStatus": "ELECTIVE",
                "estimatedDuration": 90,
                "requiredEquipment": [],
                "accessibilityRequired": False
            },
            {
                "courseId": "PHYS101",
                "courseName": "General Physics",
                "studentCount": 70,
                "professorIds": ["PROF006"],
                "mandatoryStatus": "MANDATORY",
                "estimatedDuration": 150,
                "requiredEquipment": [],
                "accessibilityRequired": False
            }
        ],
        "availableRooms": [
            {
                "roomId": "ROOM_A101",
                "roomName": "Large Amphitheater A101",
                "capacity": 150,
                "equipment": ["projector", "microphone", "computers"],
                "location": "Building A",
                "accessibility": True
            },
            {
                "roomId": "ROOM_B205",
                "roomName": "Medium Classroom B205",
                "capacity": 80,
                "equipment": ["whiteboard", "projector"],
                "location": "Building B",
                "accessibility": True
            },
            {
                "roomId": "ROOM_C302",
                "roomName": "Computer Lab C302",
                "capacity": 40,
                "equipment": ["computers", "projector"],
                "location": "Building C",
                "accessibility": True
            }
        ],
        "professorPreferences": [
            {
                "preferenceId": "PREF_COMPLEX_001",
                "professorId": "PROF001",
                "courseId": "CS101",
                "preferredDates": ["2025-06-21"],
                "preferredTimeSlots": [
                    {"startTime": "09:00:00", "endTime": "12:00:00"}
                ],
                "unavailableDates": ["2025-06-25"],
                "unavailableTimeSlots": [
                    {"startTime": "16:00:00", "endTime": "17:00:00"}
                ],
                "preferredRooms": ["ROOM_A101"],
                "specialRequirements": "Needs large room and computers",
                "priority": 1
            },
            {
                "preferenceId": "PREF_COMPLEX_002",
                "professorId": "PROF001",
                "courseId": "CS201",
                "preferredDates": ["2025-06-22"],
                "preferredTimeSlots": [
                    {"startTime": "14:00:00", "endTime": "17:00:00"}
                ],
                "unavailableDates": ["2025-06-25"],
                "unavailableTimeSlots": [
                    {"startTime": "16:00:00", "endTime": "17:00:00"}
                ],
                "preferredRooms": ["ROOM_C302"],
                "specialRequirements": "Computer lab required",
                "priority": 1
            },
            {
                "preferenceId": "PREF_COMPLEX_003",
                "professorId": "PROF003",
                "courseId": "MATH101",
                "preferredDates": ["2025-06-20", "2025-06-21"],
                "preferredTimeSlots": [
                    {"startTime": "10:00:00", "endTime": "12:00:00"},
                    {"startTime": "11:00:00", "endTime": "13:00:00"}
                ],
                "unavailableDates": [],
                "unavailableTimeSlots": [
                    {"startTime": "08:00:00", "endTime": "09:00:00"}
                ],
                "preferredRooms": [],
                "specialRequirements": "Morning preference",
                "priority": 1
            }
        ],
        "institutionalConstraints": {
            "workingHours": {
                "startTime": "08:00:00",
                "endTime": "18:00:00"
            },
            "minimumExamDuration": 90,
            "minimumGapMinutes": 30,
            "maxExamsPerDay": 4,
            "maxExamsPerRoom": 6,
            "allowWeekendExams": False
        }
    }

def create_impossible_test_case() -> Dict[str, Any]:
    """Edge case: Impossible to schedule (not enough rooms/time)"""
    return {
        "examPeriod": {
            "examSessionPeriodId": "IMPOSSIBLE_CASE",
            "academicYear": "2024-2025",
            "examSession": "Test",
            "startDate": "2025-06-20",
            "endDate": "2025-06-20"  # Only one day
        },
        "courses": [
            {
                "courseId": "CS101",
                "courseName": "Course 1",
                "studentCount": 200,  # Too many students
                "professorIds": ["PROF001"],
                "mandatoryStatus": "MANDATORY",
                "estimatedDuration": 300,  # 5 hours - too long
                "requiredEquipment": ["nonexistent_equipment"],
                "accessibilityRequired": True
            },
            {
                "courseId": "CS102",
                "courseName": "Course 2",
                "studentCount": 150,
                "professorIds": ["PROF001"],  # Same professor
                "mandatoryStatus": "MANDATORY",
                "estimatedDuration": 300,
                "requiredEquipment": [],
                "accessibilityRequired": False
            }
        ],
        "availableRooms": [
            {
                "roomId": "ROOM_SMALL",
                "roomName": "Tiny Room",
                "capacity": 10,
                "equipment": [],
                "location": "Building X",
                "accessibility": False
            }
        ],
        "professorPreferences": [],
        "institutionalConstraints": {
            "workingHours": {
                "startTime": "08:00:00",
                "endTime": "10:00:00"  # Only 2 hours available
            },
            "minimumExamDuration": 90,
            "minimumGapMinutes": 30,
            "maxExamsPerDay": 1,
            "maxExamsPerRoom": 1,
            "allowWeekendExams": False
        }
    }

def test_health_endpoint():
    """Test if the Python service is running"""
    try:
        response = requests.get(f"{BASE_URL}/api/health")
        if response.status_code == 200:
            print("✅ Health check passed")
            print(f"Response: {response.json()}")
            return True
        else:
            print(f"❌ Health check failed with status {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print("❌ Cannot connect to Python service. Is it running on port 8009?")
        return False
    except Exception as e:
        print(f"❌ Health check error: {e}")
        return False

def test_schedule_generation(test_data: Dict[str, Any], test_name: str):
    """Test schedule generation with given data"""
    print(f"\n🧪 Testing: {test_name}")

    try:
        response = requests.post(
            f"{BASE_URL}/api/schedule/generate",
            json=test_data,
            headers={"Content-Type": "application/json"},
            timeout=30
        )

        if response.status_code == 200:
            result = response.json()
            print(f"✅ {test_name} - SUCCESS")
            print(f"   Scheduled {len(result['scheduledExams'])} out of {len(test_data['courses'])} courses")
            print(f"   Quality Score: {result['qualityScore']:.2f}")
            print(f"   Processing Time: {result['processingTimeMs']}ms")
            print(f"   Violations: {len(result['violations'])}")

            if result['violations']:
                print("   Violations found:")
                for violation in result['violations']:
                    print(f"     - {violation['violationType']}: {violation['description']}")

            if result['scheduledExams']:
                print("   Sample scheduled exam:")
                exam = result['scheduledExams'][0]
                print(f"     {exam['courseId']} on {exam['examDate']} at {exam['startTime']}-{exam['endTime']} in {exam['roomName']}")

            return result
        else:
            print(f"❌ {test_name} - FAILED")
            print(f"   Status Code: {response.status_code}")
            print(f"   Response: {response.text}")
            return None

    except requests.exceptions.Timeout:
        print(f"❌ {test_name} - TIMEOUT (>30s)")
        return None
    except Exception as e:
        print(f"❌ {test_name} - ERROR: {e}")
        return None

def run_all_tests():
    """Run all test scenarios"""
    print("🚀 Starting Python Service Tests")
    print("=" * 50)

    # Test 1: Health check
    if not test_health_endpoint():
        print("\n❌ Python service is not available. Please start it first.")
        print("Run: python main.py")
        return

    # Test 2: Simple case
    simple_result = test_schedule_generation(
        create_simple_test_case(),
        "Simple Case (3 courses, 3 rooms)"
    )

    # Test 3: Complex case
    complex_result = test_schedule_generation(
        create_complex_test_case(),
        "Complex Case (6 courses, 3 rooms, conflicts)"
    )

    # Test 4: Impossible case
    impossible_result = test_schedule_generation(
        create_impossible_test_case(),
        "Impossible Case (edge case testing)"
    )

    print("\n" + "=" * 50)
    print("📊 TEST SUMMARY")
    print("=" * 50)

    tests = [
        ("Simple Case", simple_result),
        ("Complex Case", complex_result),
        ("Impossible Case", impossible_result)
    ]

    for test_name, result in tests:
        if result is not None:
            status = "✅ PASS" if result['success'] else "⚠️  PARTIAL"
            scheduled = len(result['scheduledExams'])
            quality = result['qualityScore']
            print(f"{status} {test_name}: {scheduled} exams scheduled, quality {quality:.2f}")
        else:
            print(f"❌ FAIL {test_name}: Service error")

if __name__ == "__main__":
    run_all_tests()