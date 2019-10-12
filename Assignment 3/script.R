df <-read.csv("/Users/dronemist/Desktop/IIT Delhi/5th sem/COL334/Assignment/Assignment 3/interArrivalConnection.csv", header=TRUE)
library("fitdistrplus")
plotdist(df$X, histo=TRUE, demp=TRUE)
plot(df$X)
descdist(df$X)
distributionpar(mfrow=c(2,2))
fe <-fitdist(df$X, "exp")
fe
plot( fitdist(df$X, "exp") )
