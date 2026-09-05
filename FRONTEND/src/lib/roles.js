export function dashboardPathFor(userType) {
  if (userType === "TAXI_DRIVER") return "/driver";
  if (userType === "TAXI_ASSOCIATION_ADMIN") return "/association-admin";
<<<<<<< HEAD
=======
  if (userType === "ADMIN") return "/admin";
>>>>>>> origin/main
  return "/vendor";
}
