package httpapi.authz

# HTTP Api request
import input

default allow = false

alow {
    input.path == "home"
    input.user == "john"
}
