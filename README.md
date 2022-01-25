# Foodbank-BackEnd

## Table of Contents

* [ Inspiration. ](#inspiration)
* [ About the Application. ](#about-the-application)
* [ Back End Specifics. ](#back-end-specifics)
* [ Demo. ](#demo)
    * [ Things Missing from Demo. ](#things-missing-from-the-demo)


Many of the comments made in the code was made for a competition.

## Inspiration
The COVID-19 pandemic has brought immense suffering to a lot of people. Many have lost their jobs and livelihood. 
Long lines have been reported at food banks across the nation due to this situation.

As this situation is unfolding across the country, I knew it would be possible to help both food requesters and food
banks effectively manage the food distribution process. I researched to see if there were any applications to solve this
problem, but I only saw a few apps that catered to food banks. This inspired me to create an application that could help
both food seekers and food banks avoid long lines and save time. The concept and the platform that was created can be
used to help reduce wait times in various other scenarios where minimal contact is desired and where the situation
limits the number of people at any given time to minimize COVID-19 risk. My broader goal from this inspiration is to use
this app to simplify the daily hardship and bring a sense of control to the lives of the people during and post pandemic.

## About the Application
My app is trying to bridge food banks and people that need food. The app will enable people to select a food bank, choose
food items, and a time that is convenient to pick up the items. This will also reduce the length of lines as food requesters
will be pre-scheduled, and the food bank will have time to plan for food distribution. When the food requester provides
a time slot, the food bank can either accept the time slot or propose a new slot if the pickup volume for that time is high.
An email alert is sent to the requester with the pick up details after acceptance of the request. At the allotted time,
food can be picked up without waiting. Depending on the volume of requests the food bank can plan for the number of
volunteers needed to staff the operations, request food donations, or request additional help. This visibility can make the food banks more efficient.

The requester can create an account through which they can manage and track food requests. This entire process will help
save time and avoid long lines, especially during extreme weather conditions. In addition, social distancing can be
enforced by only allowing a specific number of people to be requesting food at the same time.

## Back End Specifics

The back end was built using spring-boot, I used PostgreSQL for the database. I hosted the server using AWS elastic beanstalk.

## Demo
[![Food Pantry Pickup Demo](https://res.cloudinary.com/marcomontalbano/image/upload/v1637882133/video_to_markdown/images/youtube--XiUyRUu7idQ-c05b58ac6eb4c4700831b2b3070cd403.jpg)](https://youtu.be/XiUyRUu7idQ "Food Pantry Pickup Demo")

### Things missing from the Demo
What isn't shown is the standard log in system. You can log in with a standard user generated login. This can be made 
from the "Create An Account" tab.

After the making of the video, I added the ability to filter food banks based on your location; with this came another
way to filter food banks combining filtering by food and location. This feature uses Google's
Map API. The colors changed a little to a darker blue.