df <-read.csv("/Users/dronemist/Desktop/IIT Delhi/5th sem/COL334/Assignment/Assignment 3/interArrivalConnection.csv", header=TRUE)
library("fitdistrplus")
plotdist(df$X, histo=TRUE, demp=TRUE)
plot(df$X)
descdist(df$X)
par(mfrow=c(2,2))
fe <-fitdist(df$X, "exp")
fe
denscomp(list(fe), legendtext=c("exp"))
cdfcomp(list(fe), legendtext=c("exp"))
qqcomp(list(fe), legendtext=c("exp"))
# plot( fitdist(df$X, "exp") )